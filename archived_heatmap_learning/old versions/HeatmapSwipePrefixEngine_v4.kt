// SPDX-License-Identifier: GPL-3.0-only

// ai-note: Block 3 step 15d — prefix variants respect straight-line maxWordLength cap

package helium314.keyboard.heatmap.swipe

object HeatmapSwipePrefixEngine_v4 {

    private const val MAX_PREFIX_VARIANTS = 18

    fun buildPrefixVariants(infer: HeatmapSwipeSegmentInfer_v4.Result): List<String> {
        val letters = infer.pathLetters
        if (letters.isEmpty()) return emptyList()
        val maxLen = infer.maxWordLength
        val variants = LinkedHashSet<String>()
        addCoreVariants(variants, letters, maxLen)
        if (!infer.straightLine.locksLetterCount) {
            addDoubleLetterVariants(variants, letters, maxLen)
        }
        infer.startKeyLabel?.let { if (it.isNotEmpty()) variants.add(it) }
        return variants
            .filter { it.length in 1..maxLen.coerceAtMost(24) }
            .sortedByDescending { it.length }
            .take(MAX_PREFIX_VARIANTS)
    }

    private fun addCoreVariants(variants: LinkedHashSet<String>, letters: List<String>, maxLen: Int) {
        val full = letters.joinToString("")
        if (full.isNotEmpty() && full.length <= maxLen) variants.add(full)
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

    private fun addDoubleLetterVariants(variants: LinkedHashSet<String>, letters: List<String>, maxLen: Int) {
        for (i in 1 until letters.size) {
            val expanded = letters.take(i) + listOf(letters[i - 1]) + letters.drop(i)
            val joined = expanded.joinToString("")
            if (joined.length <= maxLen) variants.add(joined)
        }
    }
}
