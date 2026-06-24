// SPDX-License-Identifier: GPL-3.0-only

// ai-note: Block 3 step 15q — bridge v15 infer into v12 downstream modules

package helium314.keyboard.heatmap.swipe

object HeatmapSwipeInferCompat_v3 {

    @JvmStatic
    fun toV12(infer: HeatmapSwipeSegmentInfer_v15.Result): HeatmapSwipeSegmentInfer_v12.Result =
        HeatmapSwipeSegmentInfer_v12.Result(
            startKeyLabel = infer.startKeyLabel,
            pathLetters = infer.pathLetters,
            pathLettersRaw = infer.pathLettersRaw,
            endKeyLabel = infer.endKeyLabel,
            beatCount = infer.beatCount,
            beatCountRaw = infer.beatCountRaw,
            classifiedBeats = infer.classifiedBeats,
            straightLine = infer.straightLine,
            maxWordLength = infer.maxWordLength,
            normalized = infer.normalized,
            touchedLetters = infer.touchedLetters,
            touchCounts = infer.touchCounts,
            rejectedTouchLetters = infer.rejectedTouchLetters,
            strokeOrderLetters = infer.strokeOrderLetters,
        )
}
