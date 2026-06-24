// SPDX-License-Identifier: GPL-3.0-only

// ai-note: Block 3 step 15l — primary + dwell paths only for infer v10

package helium314.keyboard.heatmap.swipe

import helium314.keyboard.heatmap.learning.HeatmapCoordinateMap_v1

object HeatmapSwipePathNeighborExpand_v6 {

    private const val MAX_VARIANTS_FULL = 12
    private const val MAX_VARIANTS_LIGHT = 4

    data class Variant(
        val letters: List<String>,
        val source: String,
    )

    @JvmStatic
    fun expand(
        infer: HeatmapSwipeSegmentInfer_v10.Result,
        layout: HeatmapCoordinateMap_v1.Snapshot?,
        lightPreview: Boolean,
    ): List<Variant> {
        val out = LinkedHashSet<Variant>()
        out.add(Variant(infer.pathLetters, "primary"))
        for (dwellPath in HeatmapSwipeDwellDoubleLetter_v1.expandPaths(infer.normalized)) {
            out.add(Variant(dwellPath, "dwell"))
        }
        return if (lightPreview) out.take(MAX_VARIANTS_LIGHT) else out.take(MAX_VARIANTS_FULL)
    }

    @JvmStatic
    fun prefixStrings(variants: List<Variant>, maxLen: Int): List<String> =
        variants
            .map { it.letters.joinToString("") }
            .filter { it.isNotEmpty() && it.length <= maxLen.coerceAtMost(24) }
            .distinct()
            .sortedByDescending { it.length }
}
