// SPDX-License-Identifier: GPL-3.0-only

// ai-note: Block 3 step 15b — prefix variants from deduped path (v3 infer)

package helium314.keyboard.heatmap.swipe

object HeatmapSwipePrefixEngine_v2 {

    private const val MAX_PREFIX_VARIANTS = 12

    fun buildPrefixVariants(infer: HeatmapSwipeSegmentInfer_v3.Result): List<String> {
        val letters = infer.pathLetters
        if (letters.isEmpty()) return emptyList()
        val variants = LinkedHashSet<String>()
        val full = letters.joinToString("")
        if (full.isNotEmpty()) variants.add(full)
        val beatTake = infer.beatCount.coerceAtMost(letters.size).coerceAtLeast(1)
        variants.add(letters.take(beatTake).joinToString(""))
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
        infer.startKeyLabel?.let { if (it.isNotEmpty()) variants.add(it) }
        return variants
            .filter { it.length in 1..24 }
            .sortedByDescending { it.length }
            .take(MAX_PREFIX_VARIANTS)
    }
}
