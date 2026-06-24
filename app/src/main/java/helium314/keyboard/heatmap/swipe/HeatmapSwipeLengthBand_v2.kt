// SPDX-License-Identifier: GPL-3.0-only

// ai-note: Block 3 step 15m — softer length slack so 4-letter words from 3-key paths are viable

package helium314.keyboard.heatmap.swipe

object HeatmapSwipeLengthBand_v2 {

    @JvmStatic
    fun multiplier(candidateLength: Int, pathLetterCount: Int): Double {
        if (pathLetterCount <= 0 || candidateLength <= 0) return 1.0
        val delta = kotlin.math.abs(candidateLength - pathLetterCount)
        return when (delta) {
            0 -> 1.0
            1 -> 0.94
            2 -> 0.82
            3 -> 0.68
            else -> 0.45
        }
    }
}
