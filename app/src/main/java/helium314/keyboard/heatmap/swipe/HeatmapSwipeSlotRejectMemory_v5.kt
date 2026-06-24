// SPDX-License-Identifier: GPL-3.0-only

// ai-note: Block 3 step 15m — filterRanked overload for infer v11

package helium314.keyboard.heatmap.swipe

import helium314.keyboard.latin.SuggestedWords.SuggestedWordInfo

object HeatmapSwipeSlotRejectMemory_v5 {

    @JvmStatic
    fun filterRanked(
        ranked: List<SuggestedWordInfo>,
        infer: HeatmapSwipeSegmentInfer_v11.Result?,
    ): List<SuggestedWordInfo> =
        HeatmapSwipeSlotRejectMemory_v1.filterRanked(
            ranked,
            infer?.let { v11 ->
                HeatmapSwipeSegmentInfer_v7.Result(
                    startKeyLabel = v11.startKeyLabel,
                    pathLetters = v11.pathLetters,
                    pathLettersRaw = v11.pathLettersRaw,
                    endKeyLabel = v11.endKeyLabel,
                    beatCount = v11.beatCount,
                    beatCountRaw = v11.beatCountRaw,
                    classifiedBeats = v11.classifiedBeats,
                    straightLine = v11.straightLine,
                    maxWordLength = v11.maxWordLength,
                    normalized = v11.normalized,
                )
            },
        )
}
