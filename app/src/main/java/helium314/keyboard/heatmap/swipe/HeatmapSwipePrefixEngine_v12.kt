// SPDX-License-Identifier: GPL-3.0-only

// ai-note: Block 3 step 15m — dict-first prefixes; dict double prefixes; no dwell/corner doubles

package helium314.keyboard.heatmap.swipe

object HeatmapSwipePrefixEngine_v12 {

    private const val MAX_PREFIX_VARIANTS = 22

    fun buildPrefixVariants(infer: HeatmapSwipeSegmentInfer_v11.Result): List<String> {
        val letters = infer.pathLetters
        if (letters.isEmpty()) return emptyList()
        val pathJoin = letters.joinToString("")
        val maxPrefixLen = infer.maxWordLength
        val variants = LinkedHashSet<String>()
        if (pathJoin.isNotEmpty()) variants.add(pathJoin)
        addCoreVariants(variants, letters, maxPrefixLen)
        for (dictDouble in HeatmapSwipeDictDoublePrefix_v1.prefixVariants(letters, maxPrefixLen)) {
            variants.add(dictDouble)
        }
        return variants
            .filter { variant ->
                variant.isNotEmpty() &&
                    variant.length <= maxPrefixLen.coerceAtMost(24) &&
                    HeatmapSwipeWordTouchGate_v1.isAllowed(variant, infer.touchedLetters, emptySet())
            }
            .sortedByDescending { it.length }
            .take(MAX_PREFIX_VARIANTS)
    }

    private fun addCoreVariants(variants: LinkedHashSet<String>, letters: List<String>, maxLen: Int) {
        if (letters.size >= 2) {
            val two = letters.take(2).joinToString("")
            if (two.length <= maxLen) variants.add(two)
            val firstLast = letters.first() + letters.last()
            if (firstLast.length <= maxLen) variants.add(firstLast)
        }
        if (letters.size >= 3 && maxLen >= 3) {
            for (dropIdx in 1 until letters.lastIndex) {
                val trimmed = letters.filterIndexed { i, _ -> i != dropIdx }.joinToString("")
                if (trimmed.length in 2..maxLen) variants.add(trimmed)
            }
        }
    }
}
