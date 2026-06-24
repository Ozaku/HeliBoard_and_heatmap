// SPDX-License-Identifier: GPL-3.0-only

// ai-note: Block 3 step 15q — long paths reject fig when path is flying (6+ letters)

package helium314.keyboard.heatmap.swipe

import helium314.keyboard.latin.SuggestedWords.SuggestedWordInfo

object HeatmapSwipeMinWordLenGuard_v5 {

    private const val LONG_PATH_MIN = 5

    @JvmStatic
    fun filterRanked(
        ranked: List<SuggestedWordInfo>,
        infer: HeatmapSwipeSegmentInfer_v15.Result,
    ): List<SuggestedWordInfo> {
        if (ranked.isEmpty()) return ranked
        val minLen = resolveMinLen(infer)
        if (minLen <= 1) return ranked
        val filtered = ranked.filter { info ->
            val word = info.mWord?.toString() ?: return@filter false
            val lettersOnly = HeatmapSwipeContractionExpand_v1.lettersOnly(word)
            lettersOnly.length >= minLen
        }
        return filtered.ifEmpty { ranked.drop(1) }
    }

    private fun resolveMinLen(infer: HeatmapSwipeSegmentInfer_v15.Result): Int {
        val pathLen = infer.pathLetters.size
        if (infer.multiCorner && pathLen >= LONG_PATH_MIN) {
            return (pathLen - 1).coerceAtLeast(4)
        }
        return HeatmapSwipeMaxWordLenPolicy_v3.minOutputLen(
            infer.touchedLetters.size,
            pathLen,
            infer.beatCount,
            infer.beatCountRaw,
        )
    }
}
