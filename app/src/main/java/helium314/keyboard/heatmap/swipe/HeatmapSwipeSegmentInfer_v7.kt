// SPDX-License-Identifier: GPL-3.0-only

// ai-note: Block 3 step 15o — legacy Result type for slot-reject adapters; active infer is v12/v13

package helium314.keyboard.heatmap.swipe

import helium314.keyboard.keyboard.Keyboard
import helium314.keyboard.latin.common.InputPointers

object HeatmapSwipeSegmentInfer_v7 {

    data class Result(
        val startKeyLabel: String?,
        val pathLetters: List<String>,
        val pathLettersRaw: List<String>,
        val endKeyLabel: String?,
        val beatCount: Int,
        val beatCountRaw: Int,
        val classifiedBeats: List<HeatmapGeometryClassifier_v1.ClassifiedBeat>,
        val straightLine: HeatmapSwipeStraightLine_v1.Analysis,
        val maxWordLength: Int,
        val normalized: HeatmapPathLettersNormalize_v2.Normalized,
    )

    /** ai-note: legacy decode v10 still calls v7.infer — delegate to v8 orchestrator */
    fun infer(keyboard: Keyboard, pointers: InputPointers): Result? {
        val v8 = HeatmapSwipeSegmentInfer_v8.infer(keyboard, pointers) ?: return null
        return Result(
            startKeyLabel = v8.startKeyLabel,
            pathLetters = v8.pathLetters,
            pathLettersRaw = v8.pathLettersRaw,
            endKeyLabel = v8.endKeyLabel,
            beatCount = v8.beatCount,
            beatCountRaw = v8.beatCountRaw,
            classifiedBeats = v8.classifiedBeats,
            straightLine = v8.straightLine,
            maxWordLength = v8.maxWordLength,
            normalized = v8.normalized,
        )
    }
}
