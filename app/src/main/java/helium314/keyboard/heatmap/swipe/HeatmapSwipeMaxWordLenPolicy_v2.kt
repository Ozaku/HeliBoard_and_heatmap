// SPDX-License-Identifier: GPL-3.0-only

// ai-note: Block 3 step 15o — min len uses raw beat wiggle when path still 3 letters

package helium314.keyboard.heatmap.swipe

object HeatmapSwipeMaxWordLenPolicy_v2 {

    @JvmStatic
    fun minOutputLen(
        touchedCount: Int,
        pathLetterCount: Int,
        beatCount: Int,
        beatCountRaw: Int,
    ): Int {
        if (pathLetterCount >= 4) return pathLetterCount.coerceIn(2, 8)
        if (beatCountRaw > pathLetterCount + 1 && pathLetterCount >= 3) {
            return (pathLetterCount + 1).coerceIn(3, 8)
        }
        if (beatCountRaw >= 5 && pathLetterCount == 3) return 4
        return HeatmapSwipeMaxWordLenPolicy_v1.minOutputLen(
            touchedCount,
            pathLetterCount,
            beatCount,
        )
    }
}
