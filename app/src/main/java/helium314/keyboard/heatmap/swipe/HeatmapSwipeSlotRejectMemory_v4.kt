// SPDX-License-Identifier: GPL-3.0-only

// ai-note: Block 3 step 15l — filterRanked overload for infer v10

package helium314.keyboard.heatmap.swipe

import helium314.keyboard.latin.SuggestedWords.SuggestedWordInfo

object HeatmapSwipeSlotRejectMemory_v4 {

    @JvmStatic
    fun filterRanked(
        ranked: List<SuggestedWordInfo>,
        infer: HeatmapSwipeSegmentInfer_v10.Result?,
    ): List<SuggestedWordInfo> =
        HeatmapSwipeSlotRejectMemory_v1.filterRanked(
            ranked,
            infer?.let { v10 ->
                HeatmapSwipeSegmentInfer_v7.Result(
                    startKeyLabel = v10.startKeyLabel,
                    pathLetters = v10.pathLetters,
                    pathLettersRaw = v10.pathLettersRaw,
                    endKeyLabel = v10.endKeyLabel,
                    beatCount = v10.beatCount,
                    beatCountRaw = v10.beatCountRaw,
                    classifiedBeats = v10.classifiedBeats,
                    straightLine = v10.straightLine,
                    maxWordLength = v10.maxWordLength,
                    normalized = v10.normalized,
                )
            },
        )
}
