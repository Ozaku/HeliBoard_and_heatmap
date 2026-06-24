// SPDX-License-Identifier: GPL-3.0-only

// ai-note: Block 3 step 15n — filterRanked overload for infer v12

package helium314.keyboard.heatmap.swipe

import helium314.keyboard.latin.SuggestedWords.SuggestedWordInfo

object HeatmapSwipeSlotRejectMemory_v6 {

    @JvmStatic
    fun filterRanked(
        ranked: List<SuggestedWordInfo>,
        infer: HeatmapSwipeSegmentInfer_v12.Result?,
    ): List<SuggestedWordInfo> =
        HeatmapSwipeSlotRejectMemory_v1.filterRanked(
            ranked,
            infer?.let { v12 ->
                HeatmapSwipeSegmentInfer_v7.Result(
                    startKeyLabel = v12.startKeyLabel,
                    pathLetters = v12.pathLetters,
                    pathLettersRaw = v12.pathLettersRaw,
                    endKeyLabel = v12.endKeyLabel,
                    beatCount = v12.beatCount,
                    beatCountRaw = v12.beatCountRaw,
                    classifiedBeats = v12.classifiedBeats,
                    straightLine = v12.straightLine,
                    maxWordLength = v12.maxWordLength,
                    normalized = v12.normalized,
                )
            },
        )
}
