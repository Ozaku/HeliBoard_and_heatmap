// SPDX-License-Identifier: GPL-3.0-only

// ai-note: Phase1 step 3.3 — soft-start prefix engine for infer v19

package helium314.keyboard.heatmap.swipe

object HeatmapSwipePrefixEngine_v19 {

    private const val MAX_PREFIX_VARIANTS = 20

    fun buildPrefixVariants(
        infer: HeatmapSwipeSegmentInfer_v19.Result,
        doublePrefixIndices: Set<Int>,
        graph: HeatmapKeyNeighborGraph_v2.Graph?,
    ): List<String> {
        val letters = infer.pathLetters
        if (letters.isEmpty()) return emptyList()
        val maxPrefixLen = infer.maxWordLength
        val requireEnd = HeatmapSwipeEndLetterPolicy_v2.requiresEndMatch(
            HeatmapSwipeInferCompat_v7.toV12(infer),
        )
        val endLabel = infer.endKeyLabel
        val variants = LinkedHashSet<String>()
        val pathJoin = letters.joinToString("")
        if (pathJoin.isNotEmpty()) variants.add(pathJoin)
        variants.addAll(
            HeatmapSwipeStartLetterSoftAnchor_v1.prefixVariantsFromStart(
                letters, infer.startDistribution, maxPrefixLen.coerceAtMost(24),
            ),
        )
        addCoreVariants(variants, letters, maxPrefixLen, requireEnd, endLabel)
        for (dictDouble in HeatmapSwipeDictDoublePrefix_v3.prefixVariantsAtIndices(
                letters, maxPrefixLen, doublePrefixIndices,
            )
        ) {
            variants.add(dictDouble)
        }
        return HeatmapSwipeStartLetterSoftAnchor_v1.filterPrefixes(
            variants.filter { variant ->
                variant.isNotEmpty() &&
                    variant.length <= maxPrefixLen.coerceAtMost(24) &&
                    HeatmapSwipeWordTouchGate_v2.isAllowed(
                        variant, infer.touchedLetters, infer.startKeyLabel, emptySet(),
                    ) &&
                    (!requireEnd || HeatmapSwipeEndLetterPolicy_v2.wordEndsOnLift(variant, endLabel))
            },
            infer.startDistribution,
            graph,
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
        if (letters.size >= 2 && letters.size <= 8) {
            if (!requireEnd || letters.size < 3) {
                val two = letters.take(2).joinToString("")
                if (two.length <= maxLen) variants.add(two)
            }
            if (letters.size <= 6) {
                val firstLast = letters.first() + letters.last()
                if (firstLast.length <= maxLen) variants.add(firstLast)
            }
        }
        if (letters.size in 3..8 && maxLen >= 3 && !requireEnd) {
            for (dropIdx in 1 until letters.lastIndex) {
                val trimmed = letters.filterIndexed { i, _ -> i != dropIdx }.joinToString("")
                if (trimmed.length in 2..maxLen) variants.add(trimmed)
            }
        }
    }
}
