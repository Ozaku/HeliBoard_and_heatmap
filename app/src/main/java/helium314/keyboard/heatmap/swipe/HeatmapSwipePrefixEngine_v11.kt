// SPDX-License-Identifier: GPL-3.0-only

// ai-note: Block 3 step 15l — prefix variants for infer v10; word touch gate on every variant

package helium314.keyboard.heatmap.swipe

object HeatmapSwipePrefixEngine_v11 {

    private const val MAX_PREFIX_VARIANTS = 20

    fun buildPrefixVariants(infer: HeatmapSwipeSegmentInfer_v10.Result): List<String> {
        val letters = infer.pathLetters
        if (letters.isEmpty()) return emptyList()
        val pathJoin = letters.joinToString("")
        val variants = LinkedHashSet<String>()
        val strictTwoKey = infer.straightLine.locksLetterCount && letters.size >= 2
        val maxPrefixLen = if (strictTwoKey) {
            infer.maxWordLength.coerceAtLeast(pathJoin.length)
        } else {
            infer.maxWordLength
        }
        val dwellDoubles = HeatmapSwipeDwellDoubleLetter_v1.dwellDoubleChars(infer.normalized)
        if (pathJoin.isNotEmpty()) variants.add(pathJoin)
        for (dwellPath in HeatmapSwipeDwellDoubleLetter_v1.expandPaths(infer.normalized)) {
            variants.add(dwellPath.joinToString(""))
        }
        if (!strictTwoKey) {
            addCoreVariants(variants, letters, infer.maxWordLength)
            if (!infer.straightLine.locksLetterCount) {
                addDoubleLetterVariants(variants, letters, infer.maxWordLength)
                addInteriorDoubleVariants(variants, letters, infer.maxWordLength)
            }
            infer.startKeyLabel?.let { if (it.isNotEmpty()) variants.add(it) }
        } else {
            for (expanded in HeatmapSwipeContractionExpand_v1.expansions(pathJoin)) {
                val lettersOnly = HeatmapSwipeContractionExpand_v1.lettersOnly(expanded)
                if (lettersOnly.isNotEmpty()) variants.add(lettersOnly)
            }
        }
        return variants
            .filter { variant ->
                variant.isNotEmpty() &&
                    variant.length <= maxPrefixLen.coerceAtMost(24) &&
                    HeatmapSwipeWordTouchGate_v1.isAllowed(variant, infer.touchedLetters, dwellDoubles)
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

    private fun addDoubleLetterVariants(variants: LinkedHashSet<String>, letters: List<String>, maxLen: Int) {
        for (i in 1 until letters.size) {
            val expanded = letters.take(i) + listOf(letters[i - 1]) + letters.drop(i)
            val joined = expanded.joinToString("")
            if (joined.length <= maxLen) variants.add(joined)
        }
    }

    private fun addInteriorDoubleVariants(variants: LinkedHashSet<String>, letters: List<String>, maxLen: Int) {
        if (letters.size < 3) return
        for (idx in 1 until letters.lastIndex) {
            val expanded = letters.take(idx) + listOf(letters[idx], letters[idx]) + letters.drop(idx + 1)
            val joined = expanded.joinToString("")
            if (joined.length <= maxLen && !hasTripleRun(joined)) variants.add(joined)
        }
    }

    private fun hasTripleRun(joined: String): Boolean {
        if (joined.length < 3) return false
        for (i in 2 until joined.length) {
            if (joined[i] == joined[i - 1] && joined[i] == joined[i - 2]) return true
        }
        return false
    }
}
