// SPDX-License-Identifier: GPL-3.0-only

// ai-note: Block 3 step 15b — prefer candidates whose length ≈ deduped beat count (44_ beat rule)

package helium314.keyboard.heatmap.swipe

object HeatmapSwipeLengthBand_v1 {

    /** ai-note: multiplier applied to combined geo+dict score; ±1 soft, ≥2 strong penalty */
    @JvmStatic
    fun multiplier(candidateLength: Int, expectedBeatCount: Int): Double {
        if (expectedBeatCount <= 0 || candidateLength <= 0) return 1.0
        val delta = kotlin.math.abs(candidateLength - expectedBeatCount)
        return when (delta) {
            0 -> 1.0
            1 -> 0.82
            2 -> 0.45
            else -> 0.15
        }
    }
}
