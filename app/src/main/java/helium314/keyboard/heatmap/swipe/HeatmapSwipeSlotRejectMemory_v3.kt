// SPDX-License-Identifier: GPL-3.0-only

// ai-note: Block 3 step 15k — filterRanked overload for infer v9

package helium314.keyboard.heatmap.swipe

import helium314.keyboard.latin.SuggestedWords.SuggestedWordInfo

object HeatmapSwipeSlotRejectMemory_v3 {

    @JvmStatic
    fun filterRanked(
        ranked: List<SuggestedWordInfo>,
        infer: HeatmapSwipeSegmentInfer_v9.Result?,
    ): List<SuggestedWordInfo> =
        HeatmapSwipeSlotRejectMemory_v1.filterRanked(
            ranked,
            infer?.let { v9 ->
                HeatmapSwipeSegmentInfer_v7.Result(
                    startKeyLabel = v9.startKeyLabel,
                    pathLetters = v9.pathLetters,
                    pathLettersRaw = v9.pathLettersRaw,
                    endKeyLabel = v9.endKeyLabel,
                    beatCount = v9.beatCount,
                    beatCountRaw = v9.beatCountRaw,
                    classifiedBeats = v9.classifiedBeats,
                    straightLine = v9.straightLine,
                    maxWordLength = v9.maxWordLength,
                    normalized = v9.normalized,
                )
            },
        )
}
