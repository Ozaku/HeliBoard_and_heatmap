// SPDX-License-Identifier: GPL-3.0-only

// ai-note: v1 — ANCHOR-DRIVEN candidate pool, replacing the transit-subsequence generator
// (HeatmapTrieCandidateGen_v1) that built words from every key the finger crossed. Here the
// gesture's detected anchors (HeatmapGestureAnchors_v2) ARE the word's letters:
//   Tier STRICT   : collapsed-run letters == anchor count (doubles allowed). The normal path.
//   Tier RELAXED  : strict pool empty -> drop one interior anchor (handles a spurious stop/corner).
//   Tier TRANSIT  : still empty -> legacy neighbor-tolerant subsequence of crossed keys (last
//                   resort so the user never sees an empty list; geometrically penalized later).
// The decoder ranks STRICT above fallback tiers via FALLBACK_GEO_PENALTY. anchorKeys are the
// ground-truth intended letters and are exported for offline tuning.
// AI EDIT MAP:
//   generate()        -> Result(anchors, anchorKeys, strict, fallback, tier)
//   Result.candidates -> strict if non-empty else fallback
package helium314.keyboard.heatmap.swipe

object HeatmapAnchorCandidateGen_v1 {

    enum class Tier { STRICT, RELAXED, TRANSIT, NONE }

    class Result(
        val anchors: List<HeatmapGestureAnchors_v2.Anchor>,
        val anchorKeys: List<Char>,
        val visited: List<Char>,
        val strict: List<HeatmapLexiconTrie_v1.Candidate>,
        val fallback: List<HeatmapLexiconTrie_v1.Candidate>,
        val tier: Tier,
    ) {
        val candidates: List<HeatmapLexiconTrie_v1.Candidate>
            get() = if (strict.isNotEmpty()) strict else fallback
    }

    fun generate(
        points: List<DoubleArray>,
        timesMs: IntArray,
        keyModel: HeatmapGestureKeyModel_v1,
        trie: HeatmapLexiconTrie_v1,
        startSeed: Char?,
    ): Result {
        val c = HeatmapGestureTuningConstants_v2
        val extracted = HeatmapGestureAnchors_v2.extract(points, timesMs, keyModel)
        val anchorKeys = extracted.keys.map { it.lowercaseChar() }
        val visited = HeatmapVisitedKeySequence_v1.build(points, keyModel)

        // --- Tier STRICT ---
        var strict = emptyList<HeatmapLexiconTrie_v1.Candidate>()
        if (anchorKeys.isNotEmpty()) {
            strict = trie.collectAnchorAlignedWords(
                anchors = anchorKeys,
                neighbors = keyModel.neighbors,
                maxCandidates = c.ANCHOR_MAX_CANDIDATES,
            )
        }

        // --- Tier TRANSIT (always generate to catch skipped letters) ---
        var transit = emptyList<HeatmapLexiconTrie_v1.Candidate>()
        if (visited.isNotEmpty()) {
            val startChars = LinkedHashSet<Char>()
            val anchor = startSeed ?: anchorKeys.firstOrNull() ?: visited.first()
            startChars.add(anchor)
            startChars.addAll(keyModel.neighborsOf(anchor))
            visited.first().let {
                startChars.add(it)
                startChars.addAll(keyModel.neighborsOf(it))
            }
            transit = trie.collectSubsequenceWords(
                visited = visited,
                neighbors = keyModel.neighbors,
                startChars = startChars,
                maxLen = c.MAX_WORD_LEN,
                maxCandidates = c.FALLBACK_MAX_CANDIDATES,
            )
        }

        // --- Tier RELAXED: drop one interior anchor (a spurious detected stop/corner) ---
        var relaxed = emptyList<HeatmapLexiconTrie_v1.Candidate>()
        if (strict.isEmpty() && anchorKeys.size >= 4) {
            val seen = HashMap<String, HeatmapLexiconTrie_v1.Candidate>()
            for (i in 1 until anchorKeys.size - 1) {
                val reduced = ArrayList<Char>(anchorKeys.size - 1)
                for (k in anchorKeys.indices) if (k != i) reduced.add(anchorKeys[k])
                // Collapse accidental adjacent duplicates created by the drop.
                val collapsed = collapseAdjacent(reduced)
                val part = trie.collectAnchorAlignedWords(
                    anchors = collapsed,
                    neighbors = keyModel.neighbors,
                    maxCandidates = c.ANCHOR_MAX_CANDIDATES,
                )
                for (cand in part) seen.putIfAbsent(cand.word, cand)
                if (seen.size >= c.ANCHOR_MAX_CANDIDATES) break
            }
            relaxed = seen.values.toList()
        }

        val fallback = if (relaxed.isNotEmpty()) relaxed + transit else transit
        val tier = if (strict.isNotEmpty()) Tier.STRICT else if (relaxed.isNotEmpty()) Tier.RELAXED else if (transit.isNotEmpty()) Tier.TRANSIT else Tier.NONE

        return Result(extracted.anchors, anchorKeys, visited, strict, fallback, tier)
    }

    private fun collapseAdjacent(keys: List<Char>): List<Char> {
        if (keys.size < 2) return keys
        val out = ArrayList<Char>(keys.size)
        var last: Char? = null
        for (k in keys) {
            if (k != last) out.add(k)
            last = k
        }
        return out
    }
}
