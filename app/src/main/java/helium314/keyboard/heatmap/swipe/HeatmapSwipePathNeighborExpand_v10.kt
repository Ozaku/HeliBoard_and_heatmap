// SPDX-License-Identifier: GPL-3.0-only

// ai-note: Block 3 step 15r — wiggle-scoped dict doubles only

package helium314.keyboard.heatmap.swipe

object HeatmapSwipePathNeighborExpand_v10 {

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
        val hasWiggle = infer.pathLetters.zipWithNext().any { (a, b) -> a == b }
        val out = LinkedHashSet<Variant>()
        out.add(Variant(infer.pathLetters, "primary"))
        for (joined in HeatmapSwipeDictDoublePrefix_v2.prefixVariants(
                infer.pathLetters, maxLen, hasWiggle,
            )
        ) {
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
