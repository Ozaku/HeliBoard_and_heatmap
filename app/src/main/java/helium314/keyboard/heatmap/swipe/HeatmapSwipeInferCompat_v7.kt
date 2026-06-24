// SPDX-License-Identifier: GPL-3.0-only

// ai-note: Phase1 step 3.3 — bridge v19 infer into v12 downstream modules

package helium314.keyboard.heatmap.swipe

object HeatmapSwipeInferCompat_v7 {

    @JvmStatic
    fun toV12(infer: HeatmapSwipeSegmentInfer_v19.Result): HeatmapSwipeSegmentInfer_v12.Result =
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
            normalized = HeatmapPathLettersNormalize_v2.Normalized(
                letters = infer.normalized.letters,
                dwellHints = infer.normalized.dwellHints,
            ),
            touchedLetters = infer.touchedLetters,
            touchCounts = infer.touchCounts,
            rejectedTouchLetters = infer.rejectedTouchLetters,
            strokeOrderLetters = infer.strokeOrderLetters,
        )
}
