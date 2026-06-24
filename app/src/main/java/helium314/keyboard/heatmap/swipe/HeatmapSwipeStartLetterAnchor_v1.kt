// SPDX-License-Identifier: GPL-3.0-only

// ai-note: Block 3 step 15t — touch-down start letter anchors path + dict prefixes (blocks oils when start=w)

package helium314.keyboard.heatmap.swipe

object HeatmapSwipeStartLetterAnchor_v1 {

    private const val MAX_HOPS_EARLY = 3
    private const val UNREACHABLE = 99

    @JvmStatic
    fun anchorPathLetters(
        pathLetters: List<String>,
        startLabel: String?,
        touchedLetters: Set<String>,
    ): List<String> {
        val start = startLabel?.lowercase()?.takeIf { it.isNotEmpty() } ?: return pathLetters
        if (pathLetters.isEmpty()) return listOf(start)
        if (pathLetters.first().lowercase() == start) return pathLetters
        if (start !in touchedLetters) return pathLetters
        return listOf(start) + pathLetters.drop(1)
    }

    @JvmStatic
    fun prefixStartsWithAnchor(prefix: String, startLabel: String?): Boolean {
        val start = startLabel?.lowercase()?.takeIf { it.isNotEmpty() } ?: return prefix.isNotEmpty()
        return prefix.lowercase().startsWith(start)
    }

    @JvmStatic
    fun wordStartsWithAnchor(word: String, startLabel: String?): Boolean {
        val start = startLabel?.lowercase()?.takeIf { it.isNotEmpty() } ?: return true
        val letters = HeatmapSwipeContractionExpand_v1.lettersOnly(word)
        return letters.firstOrNull()?.toString() == start
    }

    @JvmStatic
    fun filterPrefixes(prefixes: Collection<String>, startLabel: String?): List<String> {
        val start = startLabel?.lowercase()?.takeIf { it.isNotEmpty() }
        if (start == null) return prefixes.filter { it.isNotEmpty() }.distinct()
        val anchored = prefixes.filter { it.lowercase().startsWith(start) }
        return if (anchored.isNotEmpty()) anchored else listOf(start)
    }

    @JvmStatic
    fun isEarlyLetterReachable(
        graph: HeatmapKeyNeighborGraph_v2.Graph?,
        anchor: String?,
        letter: String,
        wordIndex: Int,
    ): Boolean {
        val start = anchor?.lowercase()?.takeIf { it.isNotEmpty() } ?: return true
        val ch = letter.lowercase()
        if (wordIndex == 0) return ch == start
        val hops = hopDistance(graph, start, ch)
        return hops <= MAX_HOPS_EARLY
    }

    @JvmStatic
    fun hopDistance(
        graph: HeatmapKeyNeighborGraph_v2.Graph?,
        from: String,
        to: String,
    ): Int {
        val src = from.lowercase()
        val dst = to.lowercase()
        if (src == dst) return 0
        val g = graph ?: HeatmapKeyNeighborGraph_v2.staticQwerty()
        val queue = ArrayDeque<Pair<String, Int>>()
        val seen = HashSet<String>()
        queue.add(src to 0)
        seen.add(src)
        while (queue.isNotEmpty()) {
            val (node, depth) = queue.removeFirst()
            if (node == dst) return depth
            if (depth >= MAX_HOPS_EARLY) continue
            for (next in HeatmapKeyNeighborGraph_v2.neighborsOf(g, node)) {
                if (seen.add(next)) queue.add(next to depth + 1)
            }
        }
        return UNREACHABLE
    }
}
