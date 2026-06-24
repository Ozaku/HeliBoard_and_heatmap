// SPDX-License-Identifier: GPL-3.0-only

// ai-note: Block 3 step 15o — reject 3-letter fet when wiggle raw beats imply 4-letter feet

package helium314.keyboard.heatmap.swipe

import helium314.keyboard.latin.SuggestedWords.SuggestedWordInfo

object HeatmapSwipeMinWordLenGuard_v3 {

    @JvmStatic
    fun filterRanked(
        ranked: List<SuggestedWordInfo>,
        infer: HeatmapSwipeSegmentInfer_v13.Result,
    ): List<SuggestedWordInfo> {
        if (ranked.isEmpty()) return ranked
        val minLen = HeatmapSwipeMaxWordLenPolicy_v2.minOutputLen(
            infer.touchedLetters.size,
            infer.pathLetters.size,
            infer.beatCount,
            infer.beatCountRaw,
        )
        if (minLen <= 1) return ranked
        val filtered = ranked.filter { info ->
            val word = info.mWord?.toString() ?: return@filter false
            val lettersOnly = HeatmapSwipeContractionExpand_v1.lettersOnly(word)
            lettersOnly.length >= minLen
        }
        return filtered.ifEmpty { ranked.drop(1) }
    }
}
