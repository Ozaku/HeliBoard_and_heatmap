// SPDX-License-Identifier: GPL-3.0-only

// ai-note: Block 3 step 15g — path variants: per-index neighbor swap + yy→tt style duplicate fixes

package helium314.keyboard.heatmap.swipe

import helium314.keyboard.heatmap.learning.HeatmapCoordinateMap_v1

object HeatmapSwipePathNeighborExpand_v1 {

    private const val MAX_VARIANTS = 22
    private const val MAX_NEIGHBORS_PER_INDEX = 3

    data class Variant(
        val letters: List<String>,
        val source: String,
    )

    @JvmStatic
    fun expand(
        pathDeduped: List<String>,
        pathRaw: List<String>,
        layout: HeatmapCoordinateMap_v1.Snapshot?,
    ): List<Variant> {
        if (pathDeduped.isEmpty()) return emptyList()
        val graph = layout?.let { HeatmapKeyNeighborGraph_v1.fromLayout(it) }
            ?: HeatmapKeyNeighborGraph_v1.staticQwerty()
        val out = LinkedHashSet<Variant>()
        out.add(Variant(pathDeduped, "primary"))
        addDuplicateNeighborRepairs(out, pathRaw, graph)
        addDuplicateNeighborRepairs(out, pathDeduped, graph)
        addMissedNeighborDoubles(out, pathDeduped, graph)
        addSingleIndexNeighborSwaps(out, pathDeduped, graph)
        return out.take(MAX_VARIANTS)
    }

    /** ai-note: prefixes for dictionary lookup from expanded paths */
    @JvmStatic
    fun prefixStrings(variants: List<Variant>, maxLen: Int): List<String> {
        return variants
            .map { it.letters.joinToString("") }
            .filter { it.isNotEmpty() && it.length <= maxLen.coerceAtMost(24) }
            .distinct()
            .sortedByDescending { it.length }
    }

    private fun addSingleIndexNeighborSwaps(
        out: LinkedHashSet<Variant>,
        path: List<String>,
        graph: HeatmapKeyNeighborGraph_v1.Graph,
    ) {
        for (i in path.indices) {
            val label = path[i]
            val neighbors = HeatmapKeyNeighborGraph_v1.neighborsOf(graph, label).take(MAX_NEIGHBORS_PER_INDEX)
            for (n in neighbors) {
                val variant = path.toMutableList()
                variant[i] = n
                out.add(Variant(variant, "swap@$i:$n"))
            }
        }
    }

    private fun addDuplicateNeighborRepairs(
        out: LinkedHashSet<Variant>,
        labels: List<String>,
        graph: HeatmapKeyNeighborGraph_v1.Graph,
    ) {
        if (labels.size < 2) return
        var i = 0
        while (i < labels.lastIndex) {
            if (labels[i] != labels[i + 1]) {
                i++
                continue
            }
            val runStart = i
            while (i < labels.lastIndex && labels[i] == labels[i + 1]) i++
            val runEnd = i
            val letter = labels[runStart]
            val runLen = runEnd - runStart + 1
            val neighbors = HeatmapKeyNeighborGraph_v1.neighborsOf(graph, letter).take(MAX_NEIGHBORS_PER_INDEX)
            for (n in neighbors) {
                val variant = labels.toMutableList()
                variant[runStart] = n
                out.add(Variant(variant, "dup1@$runStart:$n"))
                if (runLen >= 2) {
                    val both = labels.toMutableList()
                    for (j in runStart..runEnd) both[j] = n
                    out.add(Variant(both, "dupRun@$runStart:$n"))
                }
            }
            i++
        }
    }

    /** ai-note: flayery + y→tt at one beat → flattery path (T missed but Y neighbor) */
    private fun addMissedNeighborDoubles(
        out: LinkedHashSet<Variant>,
        path: List<String>,
        graph: HeatmapKeyNeighborGraph_v1.Graph,
    ) {
        for (i in path.indices) {
            val label = path[i]
            for (n in HeatmapKeyNeighborGraph_v1.neighborsOf(graph, label).take(MAX_NEIGHBORS_PER_INDEX)) {
                val replaced = path.take(i) + listOf(n, n) + path.drop(i + 1)
                out.add(Variant(replaced, "missedDouble@$i:$n"))
                val inserted = path.take(i) + listOf(n, n) + path.drop(i)
                out.add(Variant(inserted, "insertDouble@$i:$n"))
            }
        }
    }
}
