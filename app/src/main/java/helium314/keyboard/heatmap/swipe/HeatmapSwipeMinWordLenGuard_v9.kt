// SPDX-License-Identifier: GPL-3.0-only

// ai-note: Phase1 step 3.3 — min len guard with soft start for infer v19

package helium314.keyboard.heatmap.swipe

import helium314.keyboard.latin.SuggestedWords.SuggestedWordInfo

object HeatmapSwipeMinWordLenGuard_v9 {

    @JvmStatic
    fun filterRanked(
        ranked: List<SuggestedWordInfo>,
        infer: HeatmapSwipeSegmentInfer_v19.Result,
    ): List<SuggestedWordInfo> {
        if (ranked.isEmpty()) return ranked
        val minLen = HeatmapSwipeMaxWordLenPolicy_v3.minOutputLen(
            infer.touchedLetters.size,
            infer.pathLetters.size,
            infer.beatCount,
            infer.beatCountRaw,
        )
        if (minLen <= 1) return ranked
        val graph = HeatmapKeyNeighborGraph_v2.staticQwerty()
        val filtered = ranked.filter { info ->
            val word = info.mWord?.toString() ?: return@filter false
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
