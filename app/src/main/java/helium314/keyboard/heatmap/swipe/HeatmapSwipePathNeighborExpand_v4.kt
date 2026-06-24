// SPDX-License-Identifier: GPL-3.0-only

// ai-note: Block 3 step 15j — neighbor swaps only within reachable pool of primary path

package helium314.keyboard.heatmap.swipe

import helium314.keyboard.heatmap.learning.HeatmapCoordinateMap_v1

object HeatmapSwipePathNeighborExpand_v4 {

    private const val MAX_VARIANTS_FULL = 14
    private const val MAX_VARIANTS_LIGHT = 4

    data class Variant(
        val letters: List<String>,
        val source: String,
    )

    @JvmStatic
    fun expand(
        infer: HeatmapSwipeSegmentInfer_v8.Result,
        layout: HeatmapCoordinateMap_v1.Snapshot?,
        lightPreview: Boolean,
    ): List<Variant> {
        val graph = layout?.let { HeatmapKeyNeighborGraph_v2.fromLayout(it) }
            ?: HeatmapKeyNeighborGraph_v2.staticQwerty()
        val pool = HeatmapSwipeReachableLetters_v2.allowedPool(infer.pathLetters, graph)
        val out = LinkedHashSet<Variant>()
        out.add(Variant(infer.pathLetters, "primary"))
        for (dwellPath in HeatmapSwipeDwellDoubleLetter_v1.expandPaths(infer.normalized)) {
            out.add(Variant(dwellPath, "dwell"))
        }
        if (lightPreview) return out.take(MAX_VARIANTS_LIGHT)
        addSingleIndexNeighborSwaps(out, infer.pathLetters, graph, pool, maxSwaps = 2)
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
        pool: Set<String>,
        maxSwaps: Int,
    ) {
        var count = 0
        for (i in path.indices) {
            if (count >= maxSwaps) break
            for (n in HeatmapKeyNeighborGraph_v2.neighborsOf(graph, path[i]).take(2)) {
                if (n !in pool) continue
                val variant = path.toMutableList()
                variant[i] = n
                out.add(Variant(variant, "swap@$i:$n"))
                count++
            }
        }
    }
}
