// SPDX-License-Identifier: GPL-3.0-only

// ai-note: Block 3 step 15n — dict double prefixes filtered by lift-end

package helium314.keyboard.heatmap.swipe

object HeatmapSwipePathNeighborExpand_v8 {

    data class Variant(
        val letters: List<String>,
        val source: String,
    )

    @JvmStatic
    fun expand(
        infer: HeatmapSwipeSegmentInfer_v12.Result,
        maxLen: Int,
    ): List<Variant> {
        val requireEnd = HeatmapSwipeEndLetterPolicy_v1.requiresEndMatch(infer)
        val endLabel = infer.endKeyLabel
        val out = LinkedHashSet<Variant>()
        out.add(Variant(infer.pathLetters, "primary"))
        for (joined in HeatmapSwipeDictDoublePrefix_v1.prefixVariants(infer.pathLetters, maxLen)) {
            if (requireEnd && !HeatmapSwipeEndLetterPolicy_v1.wordEndsOnLift(joined, endLabel)) continue
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
