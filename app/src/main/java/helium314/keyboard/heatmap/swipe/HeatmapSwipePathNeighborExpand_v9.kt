// SPDX-License-Identifier: GPL-3.0-only

// ai-note: Block 3 step 15p — neighbor expand uses scoped end policy v2

package helium314.keyboard.heatmap.swipe

object HeatmapSwipePathNeighborExpand_v9 {

    data class Variant(
        val letters: List<String>,
        val source: String,
    )

    @JvmStatic
    fun expand(
        infer: HeatmapSwipeSegmentInfer_v12.Result,
        maxLen: Int,
    ): List<Variant> {
        val requireEnd = HeatmapSwipeEndLetterPolicy_v2.requiresEndMatch(infer)
        val endLabel = infer.endKeyLabel
        val out = LinkedHashSet<Variant>()
        out.add(Variant(infer.pathLetters, "primary"))
        for (joined in HeatmapSwipeDictDoublePrefix_v1.prefixVariants(infer.pathLetters, maxLen)) {
            if (requireEnd && !HeatmapSwipeEndLetterPolicy_v2.wordEndsOnLift(joined, endLabel)) continue
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
