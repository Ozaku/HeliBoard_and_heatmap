// SPDX-License-Identifier: GPL-3.0-only

// ai-note: Block 3 step 15t — prefixes must start with touch-down start; dict lookup anchored

package helium314.keyboard.heatmap.swipe

object HeatmapSwipePrefixEngine_v18 {

    private const val MAX_PREFIX_VARIANTS = 18

    fun buildPrefixVariants(
        infer: HeatmapSwipeSegmentInfer_v12.Result,
        doublePrefixIndices: Set<Int>,
    ): List<String> {
        val letters = infer.pathLetters
        if (letters.isEmpty()) return emptyList()
        val start = infer.startKeyLabel
        val maxPrefixLen = infer.maxWordLength
        val requireEnd = HeatmapSwipeEndLetterPolicy_v2.requiresEndMatch(infer)
        val endLabel = infer.endKeyLabel
        val variants = LinkedHashSet<String>()
        val pathJoin = letters.joinToString("")
        if (pathJoin.isNotEmpty()) variants.add(pathJoin)
        addCoreVariants(variants, letters, maxPrefixLen, requireEnd, endLabel)
        for (dictDouble in HeatmapSwipeDictDoublePrefix_v3.prefixVariantsAtIndices(
                letters, maxPrefixLen, doublePrefixIndices,
            )
        ) {
            variants.add(dictDouble)
        }
        return HeatmapSwipeStartLetterAnchor_v1.filterPrefixes(
            variants.filter { variant ->
                variant.isNotEmpty() &&
                    variant.length <= maxPrefixLen.coerceAtMost(24) &&
                    HeatmapSwipeWordTouchGate_v2.isAllowed(
                        variant, infer.touchedLetters, start, emptySet(),
                    ) &&
                    (!requireEnd || HeatmapSwipeEndLetterPolicy_v2.wordEndsOnLift(variant, endLabel))
            },
            start,
        )
            .sortedByDescending { it.length }
            .take(MAX_PREFIX_VARIANTS)
    }

    private fun addCoreVariants(
        variants: LinkedHashSet<String>,
        letters: List<String>,
        maxLen: Int,
        requireEnd: Boolean,
        endLabel: String?,
    ) {
        if (letters.size >= 2 && letters.size <= 6) {
            if (!requireEnd || letters.size < 3) {
                val two = letters.take(2).joinToString("")
                if (two.length <= maxLen) variants.add(two)
            }
            if (letters.size <= 5) {
                val firstLast = letters.first() + letters.last()
                if (firstLast.length <= maxLen) variants.add(firstLast)
            }
        }
        if (letters.size in 3..6 && maxLen >= 3 && !requireEnd) {
            for (dropIdx in 1 until letters.lastIndex) {
                val trimmed = letters.filterIndexed { i, _ -> i != dropIdx }.joinToString("")
                if (trimmed.length in 2..maxLen) variants.add(trimmed)
            }
        }
    }
}
