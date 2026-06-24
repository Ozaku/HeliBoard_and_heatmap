// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.heatmap.swipe

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class HeatmapLiteralSwipeScore_v9Test {

    private val pathFet = listOf("f", "e", "t")
    private val dwellE = setOf('e')

    @Test
    fun rejectsFatOnFet() {
        val score = HeatmapLiteralSwipeScore_v9.scoreWord(
            candidate = "fat",
            pathLetters = pathFet,
            startLabel = "f",
            endLabel = "t",
            dwellDoubleLetters = dwellE,
        )
        assertEquals(Double.NEGATIVE_INFINITY, score, 0.0)
    }

    @Test
    fun feetBeatsFitWithoutNeighborCheating() {
        val feet = HeatmapLiteralSwipeScore_v9.scoreWord(
            candidate = "feet",
            pathLetters = pathFet,
            startLabel = "f",
            endLabel = "t",
            dwellDoubleLetters = dwellE,
        )
        val fit = HeatmapLiteralSwipeScore_v9.scoreWord(
            candidate = "fit",
            pathLetters = pathFet,
            startLabel = "f",
            endLabel = "t",
            dwellDoubleLetters = dwellE,
        )
        assertTrue(feet > 0.0)
        assertEquals(Double.NEGATIVE_INFINITY, fit, 0.0)
    }
}
