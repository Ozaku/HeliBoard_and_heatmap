// SPDX-License-Identifier: GPL-3.0-only

// ai-note: Block 3 step 15h — light preview mode skips heavy neighbor expand (fixes 5+ char lag)

package helium314.keyboard.heatmap.swipe

import helium314.keyboard.heatmap.learning.HeatmapCoordinateMap_v1

object HeatmapSwipePathNeighborExpand_v2 {

    private const val MAX_VARIANTS_FULL = 18
    private const val MAX_VARIANTS_LIGHT = 6
    private const val MAX_NEIGHBORS_PER_INDEX = 2

    data class Variant(
        val letters: List<String>,
        val source: String,
    )

    @JvmStatic
    fun expand(
        pathDeduped: List<String>,
        pathRaw: List<String>,
        layout: HeatmapCoordinateMap_v1.Snapshot?,
        lightPreview: Boolean,
    ): List<Variant> {
        if (pathDeduped.isEmpty()) return emptyList()
        val graph = layout?.let { HeatmapKeyNeighborGraph_v2.fromLayout(it) }
            ?: HeatmapKeyNeighborGraph_v2.staticQwerty()
        val out = LinkedHashSet<Variant>()
        out.add(Variant(pathDeduped, "primary"))
        if (lightPreview) {
            addMissedNeighborDoubles(out, pathDeduped, graph, maxInserts = 2)
            return out.take(MAX_VARIANTS_LIGHT)
        }
        addDuplicateNeighborRepairs(out, pathRaw, graph)
        addDuplicateNeighborRepairs(out, pathDeduped, graph)
        addMissedNeighborDoubles(out, pathDeduped, graph, maxInserts = 4)
        addSingleIndexNeighborSwaps(out, pathDeduped, graph, maxSwaps = 4)
        return out.take(MAX_VARIANTS_FULL)
    }

    @JvmStatic
    fun prefixStrings(variants: List<Variant>, maxLen: Int): List<String> =
        variants
            .map { it.letters.joinToString("") }
            .filter { it.isNotEmpty() && it.length <= maxLen.coerceAtMost(24) }
            .distinct()
            .sortedByDescending { it.length }

    private fun addSingleIndexNeighborSwaps(
        out: LinkedHashSet<Variant>,
        path: List<String>,
        graph: HeatmapKeyNeighborGraph_v2.Graph,
        maxSwaps: Int,
    ) {
        var count = 0
        for (i in path.indices) {
            if (count >= maxSwaps) break
            val neighbors = HeatmapKeyNeighborGraph_v2.neighborsOf(graph, path[i]).take(MAX_NEIGHBORS_PER_INDEX)
            for (n in neighbors) {
                val variant = path.toMutableList()
                variant[i] = n
                out.add(Variant(variant, "swap@$i:$n"))
                count++
            }
        }
    }

    private fun addDuplicateNeighborRepairs(
        out: LinkedHashSet<Variant>,
        labels: List<String>,
        graph: HeatmapKeyNeighborGraph_v2.Graph,
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
            val letter = labels[runStart]
            for (n in HeatmapKeyNeighborGraph_v2.neighborsOf(graph, letter).take(MAX_NEIGHBORS_PER_INDEX)) {
                val variant = labels.toMutableList()
                variant[runStart] = n
                out.add(Variant(variant, "dup1@$runStart:$n"))
                val both = labels.toMutableList()
                for (j in runStart..i) both[j] = n
                out.add(Variant(both, "dupRun@$runStart:$n"))
            }
            i++
        }
    }

    private fun addMissedNeighborDoubles(
        out: LinkedHashSet<Variant>,
        path: List<String>,
        graph: HeatmapKeyNeighborGraph_v2.Graph,
        maxInserts: Int,
    ) {
        var count = 0
        for (i in path.indices) {
            if (count >= maxInserts) break
            for (n in HeatmapKeyNeighborGraph_v2.neighborsOf(graph, path[i]).take(MAX_NEIGHBORS_PER_INDEX)) {
                val replaced = path.take(i) + listOf(n, n) + path.drop(i + 1)
                out.add(Variant(replaced, "missedDouble@$i:$n"))
                count++
            }
        }
    }
}
