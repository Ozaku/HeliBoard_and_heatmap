// SPDX-License-Identifier: GPL-3.0-only

// ai-note: Block 3 step 15q — long multi-corner paths use full path prefixes only, not fg/fig shortcuts

package helium314.keyboard.heatmap.swipe

object HeatmapSwipePrefixEngine_v15 {

    private const val MAX_PREFIX_VARIANTS = 24
    private const val LONG_PATH_MIN = 5

    fun buildPrefixVariants(infer: HeatmapSwipeSegmentInfer_v12.Result): List<String> {
        val letters = infer.pathLetters
        if (letters.isEmpty()) return emptyList()
        val pathJoin = letters.joinToString("")
        val maxPrefixLen = infer.maxWordLength
        val requireEnd = HeatmapSwipeEndLetterPolicy_v2.requiresEndMatch(infer)
        val endLabel = infer.endKeyLabel
        val longPath = letters.size >= LONG_PATH_MIN
        val variants = LinkedHashSet<String>()
        if (pathJoin.isNotEmpty()) variants.add(pathJoin)
        if (longPath) {
            addProgressivePrefixes(variants, letters, maxPrefixLen)
        } else {
            addCoreVariants(variants, letters, maxPrefixLen, requireEnd, endLabel)
            for (dictDouble in HeatmapSwipeDictDoublePrefix_v1.prefixVariants(letters, maxPrefixLen)) {
                variants.add(dictDouble)
            }
        }
        return variants
            .filter { variant ->
                variant.isNotEmpty() &&
                    variant.length <= maxPrefixLen.coerceAtMost(24) &&
                    HeatmapSwipeWordTouchGate_v1.isAllowed(variant, infer.touchedLetters, emptySet()) &&
                    (!requireEnd || HeatmapSwipeEndLetterPolicy_v2.wordEndsOnLift(variant, endLabel))
            }
            .sortedByDescending { it.length }
            .take(MAX_PREFIX_VARIANTS)
    }

    private fun addProgressivePrefixes(
        variants: LinkedHashSet<String>,
        letters: List<String>,
        maxLen: Int,
    ) {
        val join = StringBuilder()
        for (letter in letters) {
            join.append(letter)
            if (join.length >= 3 && join.length <= maxLen) {
                variants.add(join.toString())
            }
        }
    }

    private fun addCoreVariants(
        variants: LinkedHashSet<String>,
        letters: List<String>,
        maxLen: Int,
        requireEnd: Boolean,
        endLabel: String?,
    ) {
        if (letters.size >= 2) {
            if (!requireEnd || letters.size < 3) {
                val two = letters.take(2).joinToString("")
                if (two.length <= maxLen) variants.add(two)
            }
            val firstLast = letters.first() + letters.last()
            if (firstLast.length <= maxLen) variants.add(firstLast)
        }
        if (letters.size >= 3 && maxLen >= 3 && !requireEnd) {
            for (dropIdx in 1 until letters.lastIndex) {
                val trimmed = letters.filterIndexed { i, _ -> i != dropIdx }.joinToString("")
                if (trimmed.length in 2..maxLen) variants.add(trimmed)
            }
        }
    }
}
