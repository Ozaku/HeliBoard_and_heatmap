// SPDX-License-Identifier: GPL-3.0-only

// ai-note: Block 3 step 15c — prefix variants incl. double-letter inserts for acc→accurate style words

package helium314.keyboard.heatmap.swipe

object HeatmapSwipePrefixEngine_v3 {

    private const val MAX_PREFIX_VARIANTS = 18

    fun buildPrefixVariants(infer: HeatmapSwipeSegmentInfer_v3.Result): List<String> {
        val letters = infer.pathLetters
        if (letters.isEmpty()) return emptyList()
        val variants = LinkedHashSet<String>()
        addCoreVariants(variants, letters)
        addDoubleLetterVariants(variants, letters)
        infer.startKeyLabel?.let { if (it.isNotEmpty()) variants.add(it) }
        return variants
            .filter { it.length in 1..24 }
            .sortedByDescending { it.length }
            .take(MAX_PREFIX_VARIANTS)
    }

    private fun addCoreVariants(variants: LinkedHashSet<String>, letters: List<String>) {
        val full = letters.joinToString("")
        if (full.isNotEmpty()) variants.add(full)
        if (letters.size >= 2) {
            variants.add(letters.take(2).joinToString(""))
            variants.add(letters.first() + letters.last())
        }
        if (letters.size >= 3) {
            for (dropIdx in 1 until letters.lastIndex) {
                val trimmed = letters.filterIndexed { i, _ -> i != dropIdx }.joinToString("")
                if (trimmed.length >= 2) variants.add(trimmed)
            }
        }
    }

    /** ai-note: single path c + swipe onward → trie may need cc (accurate, account, letter, …) */
    private fun addDoubleLetterVariants(variants: LinkedHashSet<String>, letters: List<String>) {
        for (i in 1 until letters.size) {
            val letter = letters[i - 1]
            val expanded = letters.take(i) + listOf(letter) + letters.drop(i)
            variants.add(expanded.joinToString(""))
        }
    }
}
