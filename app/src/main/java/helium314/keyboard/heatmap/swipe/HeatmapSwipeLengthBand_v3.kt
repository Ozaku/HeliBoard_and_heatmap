// SPDX-License-Identifier: GPL-3.0-only

// ai-note: v3 — softer length penalty when visit trace is longer than collapsed intent path

package helium314.keyboard.heatmap.swipe

object HeatmapSwipeLengthBand_v3 {

    @JvmStatic
    fun multiplier(
        candidateLength: Int,
        pathLetterCount: Int,
        visitLetterCount: Int = pathLetterCount,
    ): Double {
        if (pathLetterCount <= 0 || candidateLength <= 0) return 1.0
        val refLen = if (visitLetterCount > pathLetterCount + 1) {
            maxOf(pathLetterCount, minOf(visitLetterCount, candidateLength))
        } else {
            pathLetterCount
        }
        val delta = kotlin.math.abs(candidateLength - refLen)
        return when (delta) {
            0 -> 1.0
            1 -> 0.94
            2 -> 0.86
            3 -> 0.74
            else -> 0.58
        }
    }
}

