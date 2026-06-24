// SPDX-License-Identifier: GPL-3.0-only

// ai-note: Block 3 step 15m — dict decode never locks to 2 letters; max len from touched path + slack

package helium314.keyboard.heatmap.swipe

object HeatmapSwipeMaxWordLenPolicy_v1 {

    private const val LEN_SLACK = 2
    private const val MIN_LEN = 2
    private const val MAX_CAP = 20

    @JvmStatic
    fun maxDictLen(pathLetterCount: Int, beatCount: Int, touchedCount: Int): Int {
        val base = maxOf(pathLetterCount, beatCount, touchedCount)
        return (base + LEN_SLACK).coerceIn(MIN_LEN, MAX_CAP)
    }

    @JvmStatic
    fun minOutputLen(touchedCount: Int, pathLetterCount: Int, beatCount: Int): Int {
        if (touchedCount >= 2 || pathLetterCount >= 2 || beatCount >= 2) return 2
        return 1
    }
}
