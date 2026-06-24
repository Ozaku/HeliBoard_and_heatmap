// SPDX-License-Identifier: GPL-3.0-only

// ai-note: v1 — candidate word pool for a gesture via visited-key-constrained lexicon search.
// Builds the visited-key sequence, derives the allowed start letters (start key + neighbors,
// honoring the LOCKED first-letter anchor), and asks the lexicon trie for all in-order
// neighbor-tolerant subsequence words. Geometric ranking happens later in the decoder.
package helium314.keyboard.heatmap.swipe

object HeatmapTrieCandidateGen_v1 {

    class Result(
        val visited: List<Char>,
        val startChars: Set<Char>,
        val candidates: List<HeatmapLexiconTrie_v1.Candidate>,
    )

    fun generate(
        points: List<DoubleArray>,
        keyModel: HeatmapGestureKeyModel_v1,
        trie: HeatmapLexiconTrie_v1,
        startSeed: Char?,
    ): Result {
        val visited = HeatmapVisitedKeySequence_v1.build(points, keyModel)
        val startChars = LinkedHashSet<Char>()
        val anchor = startSeed ?: visited.firstOrNull()
        if (anchor != null) {
            startChars.add(anchor)
            startChars.addAll(keyModel.neighborsOf(anchor))
        }
        // Always allow the actual first visited key too.
        visited.firstOrNull()?.let {
            startChars.add(it)
            startChars.addAll(keyModel.neighborsOf(it))
        }
        val candidates = trie.collectSubsequenceWords(
            visited = visited,
            neighbors = keyModel.neighbors,
            startChars = startChars,
            maxLen = HeatmapGestureTuningConstants_v1.MAX_WORD_LEN,
            maxCandidates = HeatmapGestureTuningConstants_v1.MAX_CANDIDATES,
        )
        return Result(visited, startChars, candidates)
    }
}
