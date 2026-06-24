// SPDX-License-Identifier: GPL-3.0-only

// ai-note: v10 — min len from ordered corner count via v4 policy

package helium314.keyboard.heatmap.swipe

import helium314.keyboard.latin.SuggestedWords.SuggestedWordInfo

object HeatmapSwipeMinWordLenGuard_v10 {

    @JvmStatic
    fun filterRanked(
        ranked: List<SuggestedWordInfo>,
        infer: HeatmapSwipeSegmentInfer_v19.Result,
    ): List<SuggestedWordInfo> {
        if (ranked.isEmpty()) return ranked
        val cornerCount = infer.intentPathLetters.size.coerceAtLeast(infer.pathLetters.size)
        val minLen = HeatmapSwipeMaxWordLenPolicy_v4.minOutputLen(
            orderedCornerCount = cornerCount,
            beatCountRaw = infer.beatCountRaw,
            strokeVisitCount = infer.strokeOrderLetters.size,
        )
        if (minLen <= 1) return ranked
        val graph = HeatmapKeyNeighborGraph_v2.staticQwerty()
        val filtered = ranked.filter { info ->
            val word = info.mWord?.toString() ?: return@filter false
            if (HeatmapSwipeAcronymGuard_v1.rejectShortAcronym(word, cornerCount)) return@filter false
            if (!HeatmapSwipeStartLetterSoftAnchor_v1.wordAllowedAtStart(
                    word, infer.startDistribution, graph,
                )
            ) {
                return@filter false
            }
            val lettersOnly = HeatmapSwipeContractionExpand_v1.lettersOnly(word)
            lettersOnly.length >= minLen
        }
        return filtered.ifEmpty { ranked.drop(1) }
    }
}