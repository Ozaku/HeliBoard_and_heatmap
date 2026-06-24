// SPDX-License-Identifier: GPL-3.0-only

// ai-note: Block 3 step 15p — min-len boost only for short wiggle paths, not every 4+ letter word

package helium314.keyboard.heatmap.swipe

object HeatmapSwipeMaxWordLenPolicy_v3 {

    @JvmStatic
    fun minOutputLen(
        touchedCount: Int,
        pathLetterCount: Int,
        beatCount: Int,
        beatCountRaw: Int,
    ): Int {
        if (pathLetterCount in 3..4 && beatCountRaw > pathLetterCount + 1) {
            return (pathLetterCount + 1).coerceIn(3, 5)
        }
        if (pathLetterCount == 3 && beatCountRaw >= 5) return 4
        return HeatmapSwipeMaxWordLenPolicy_v1.minOutputLen(
            touchedCount,
            pathLetterCount,
            beatCount,
        )
    }
}
