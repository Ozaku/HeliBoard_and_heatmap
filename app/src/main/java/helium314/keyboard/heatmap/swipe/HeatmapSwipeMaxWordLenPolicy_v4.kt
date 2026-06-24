// SPDX-License-Identifier: GPL-3.0-only

// ai-note: v4 — min output length from ordered corner count (blocks 2-letter junk on long swipes)

package helium314.keyboard.heatmap.swipe

object HeatmapSwipeMaxWordLenPolicy_v4 {

    @JvmStatic
    fun minOutputLen(
        orderedCornerCount: Int,
        beatCountRaw: Int,
        strokeVisitCount: Int,
    ): Int {
        if (orderedCornerCount >= 4) return orderedCornerCount.coerceAtMost(12)
        if (orderedCornerCount == 3) return 3
        if (orderedCornerCount >= 2 && beatCountRaw >= 4) return 2
        if (strokeVisitCount >= 6 && orderedCornerCount >= 2) return 2
        return 1
    }
}