// SPDX-License-Identifier: GPL-3.0-only

// ai-note: Block 3 step 15m — primary path + dict double prefixes only

package helium314.keyboard.heatmap.swipe

import helium314.keyboard.heatmap.learning.HeatmapCoordinateMap_v1

object HeatmapSwipePathNeighborExpand_v7 {

    data class Variant(
        val letters: List<String>,
        val source: String,
    )

    @JvmStatic
    fun expand(
        infer: HeatmapSwipeSegmentInfer_v11.Result,
        maxLen: Int,
    ): List<Variant> {
        val out = LinkedHashSet<Variant>()
        out.add(Variant(infer.pathLetters, "primary"))
        for (joined in HeatmapSwipeDictDoublePrefix_v1.prefixVariants(infer.pathLetters, maxLen)) {
            out.add(Variant(joined.map { it.toString() }, "dictDouble"))
        }
        return out.toList()
    }

    @JvmStatic
    fun prefixStrings(variants: List<Variant>, maxLen: Int): List<String> =
        variants
            .map { it.letters.joinToString("") }
            .filter { it.isNotEmpty() && it.length <= maxLen.coerceAtMost(24) }
            .distinct()
            .sortedByDescending { it.length }
}
