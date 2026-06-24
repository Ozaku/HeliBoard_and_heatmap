// SPDX-License-Identifier: GPL-3.0-only

// ai-note: v8 — downstream modules use intent path as primary letter sequence

package helium314.keyboard.heatmap.swipe

object HeatmapSwipeInferCompat_v8 {

    @JvmStatic
    fun toV12(infer: HeatmapSwipeSegmentInfer_v19.Result): HeatmapSwipeSegmentInfer_v12.Result =
        HeatmapSwipeInferCompat_v7.toV12(infer)

    @JvmStatic
    fun intentPrimaryV12(infer: HeatmapSwipeSegmentInfer_v19.Result): HeatmapSwipeSegmentInfer_v12.Result {
        val base = HeatmapSwipeInferCompat_v7.toV12(infer)
        if (infer.intentPathLetters.isEmpty()) return base
        return base.copy(pathLetters = infer.intentPathLetters)
    }
}
