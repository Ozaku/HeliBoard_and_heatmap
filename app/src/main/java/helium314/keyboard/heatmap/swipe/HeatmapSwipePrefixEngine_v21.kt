// SPDX-License-Identifier: GPL-3.0-only

// ai-note: v21 — progressive ordered-corner prefixes only; no shuffled or visit-expanded variants

package helium314.keyboard.heatmap.swipe

object HeatmapSwipePrefixEngine_v21 {

    private const val MAX_PREFIX_VARIANTS = 16

    fun buildPrefixVariants(
        infer: HeatmapSwipeSegmentInfer_v19.Result,
        doublePrefixIndices: Set<Int>,
        graph: HeatmapKeyNeighborGraph_v2.Graph?,
    ): List<String> {
        val ordered = infer.intentPathLetters.ifEmpty { infer.pathLetters }
        if (ordered.isEmpty()) return emptyList()
        val maxPrefixLen = infer.maxWordLength.coerceAtMost(24)
        val requireEnd = HeatmapSwipeEndLetterPolicy_v3.requiresEndMatch(infer)
        val endLabel = infer.endKeyLabel
        val variants = LinkedHashSet<String>()

        variants.addAll(
            HeatmapSwipeStrokeMonotonicPath_v1.progressivePrefixes(ordered, maxPrefixLen),
        )
        variants.addAll(
            HeatmapSwipeStartLetterSoftAnchor_v1.prefixVariantsFromStart(
                ordered, infer.startDistribution, maxPrefixLen,
            ),
        )
        for (dictDouble in HeatmapSwipeDictDoublePrefix_v3.prefixVariantsAtIndices(
                ordered, maxPrefixLen, doublePrefixIndices,
            )
        ) {
            if (HeatmapSwipeStrokeMonotonicPath_v1.isMonotonicSubsequence(dictDouble, ordered)) {
                variants.add(dictDouble)
            }
        }

        return HeatmapSwipeStartLetterSoftAnchor_v1.filterPrefixes(
            HeatmapSwipeStrokeMonotonicPath_v1.filterPrefixes(
                variants.filter { variant ->
                    variant.isNotEmpty() &&
                        variant.length <= maxPrefixLen &&
                        HeatmapSwipeWordTouchGate_v2.isAllowed(
                            variant, infer.touchedLetters, infer.startKeyLabel, emptySet(),
                        ) &&
                        (!requireEnd || HeatmapSwipeEndLetterPolicy_v3.wordEndsOnLift(variant, endLabel, graph))
                },
                ordered,
            ),
            infer.startDistribution,
            graph,
        )
            .sortedByDescending { it.length }
            .take(MAX_PREFIX_VARIANTS)
    }
}