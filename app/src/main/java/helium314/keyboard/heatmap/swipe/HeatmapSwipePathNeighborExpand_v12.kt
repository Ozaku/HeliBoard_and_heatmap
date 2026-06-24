// SPDX-License-Identifier: GPL-3.0-only

// ai-note: v12 — neighbor expand uses intent-primary path letters

package helium314.keyboard.heatmap.swipe

object HeatmapSwipePathNeighborExpand_v12 {

    data class Variant(
        val letters: List<String>,
        val source: String,
    )

    @JvmStatic
    fun expand(
        infer: HeatmapSwipeSegmentInfer_v19.Result,
        maxLen: Int,
        doublePrefixIndices: Set<Int>,
    ): List<Variant> {
        val inferV12 = HeatmapSwipeInferCompat_v8.intentPrimaryV12(infer)
        val requireEnd = HeatmapSwipeEndLetterPolicy_v3.requiresEndMatch(infer)
        val endLabel = infer.endKeyLabel
        val out = LinkedHashSet<Variant>()
        out.add(Variant(inferV12.pathLetters, "intentPrimary"))
        for (joined in HeatmapSwipeDictDoublePrefix_v3.prefixVariantsAtIndices(
                inferV12.pathLetters, maxLen, doublePrefixIndices,
            )
        ) {
            if (requireEnd && !HeatmapSwipeEndLetterPolicy_v3.wordEndsOnLift(joined, endLabel)) continue
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
