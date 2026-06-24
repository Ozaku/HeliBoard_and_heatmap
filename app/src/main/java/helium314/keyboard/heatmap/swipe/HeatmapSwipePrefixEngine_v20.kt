// SPDX-License-Identifier: GPL-3.0-only

// ai-note: v20 — intent path primary dictionary lookup; normalized path limited fallback

package helium314.keyboard.heatmap.swipe

object HeatmapSwipePrefixEngine_v20 {

    private const val MAX_PREFIX_VARIANTS = 20
    private const val MAX_NORMALIZED_FALLBACK = 3

    fun buildPrefixVariants(
        infer: HeatmapSwipeSegmentInfer_v19.Result,
        doublePrefixIndices: Set<Int>,
        graph: HeatmapKeyNeighborGraph_v2.Graph?,
    ): List<String> {
        val intent = infer.intentPathLetters
        val normalized = infer.pathLetters
        val primary = if (intent.isNotEmpty()) intent else normalized
        if (primary.isEmpty()) return emptyList()

        val maxPrefixLen = infer.maxWordLength.coerceAtMost(24)
        val requireEnd = HeatmapSwipeEndLetterPolicy_v3.requiresEndMatch(infer)
        val endLabel = infer.endKeyLabel
        val variants = LinkedHashSet<String>()

        addIntentPrimaryVariants(variants, intent, normalized, maxPrefixLen, requireEnd, endLabel)
        addCoreVariants(variants, primary, maxPrefixLen, requireEnd, endLabel)
        for (dictDouble in HeatmapSwipeDictDoublePrefix_v3.prefixVariantsAtIndices(
                primary, maxPrefixLen, doublePrefixIndices,
            )
        ) {
            variants.add(dictDouble)
        }
        variants.addAll(
            HeatmapSwipeStartLetterSoftAnchor_v1.prefixVariantsFromStart(
                primary, infer.startDistribution, maxPrefixLen,
            ),
        )

        return HeatmapSwipeStartLetterSoftAnchor_v1.filterPrefixes(
            variants.filter { variant ->
                variant.isNotEmpty() &&
                    variant.length <= maxPrefixLen &&
                    HeatmapSwipeWordTouchGate_v2.isAllowed(
                        variant, infer.touchedLetters, infer.startKeyLabel, emptySet(),
                    ) &&
                    (!requireEnd || HeatmapSwipeEndLetterPolicy_v3.wordEndsOnLift(variant, endLabel, graph))
            },
            infer.startDistribution,
            graph,
        )
            .sortedWith(
                compareByDescending<String> { prefix ->
                    if (intent.isNotEmpty() && prefix.startsWith(intent.joinToString(""))) 2 else 0
                }.thenByDescending { it.length },
            )
            .take(MAX_PREFIX_VARIANTS)
    }

    private fun addIntentPrimaryVariants(
        variants: LinkedHashSet<String>,
        intent: List<String>,
        normalized: List<String>,
        maxLen: Int,
        requireEnd: Boolean,
        endLabel: String?,
    ) {
        if (intent.isEmpty()) return
        val intentStr = intent.joinToString("")
        if (intentStr.isNotEmpty() && intentStr.length <= maxLen) variants.add(intentStr)
        for (len in intent.size downTo 2) {
            val prefix = intent.take(len).joinToString("")
            if (prefix.length <= maxLen) variants.add(prefix)
        }
        if (intent.size >= 3) {
            val firstLast = intent.first() + intent.last()
            if (firstLast.length <= maxLen &&
                (!requireEnd || HeatmapSwipeEndLetterPolicy_v3.wordEndsOnLift(firstLast, endLabel))
            ) {
                variants.add(firstLast)
            }
        }
        if (normalized.isNotEmpty() && normalized.joinToString("") != intentStr) {
            var fallbackCount = 0
            val normStr = normalized.joinToString("")
            if (normStr.length <= maxLen) {
                variants.add(normStr)
                fallbackCount++
            }
            if (fallbackCount < MAX_NORMALIZED_FALLBACK && normalized.size >= 3) {
                val trimmed = normalized.take(normalized.size - 1).joinToString("")
                if (trimmed.length in 2..maxLen) variants.add(trimmed)
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
        if (letters.size >= 2 && letters.size <= 8) {
            if (!requireEnd || letters.size < 3) {
                val two = letters.take(2).joinToString("")
                if (two.length <= maxLen) variants.add(two)
            }
        }
    }
}
