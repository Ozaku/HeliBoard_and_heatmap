// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.heatmap.swipe

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class HeatmapLiteralSwipeScore_v5Test {

    private val pathFlayery = listOf("f", "l", "a", "y", "e", "r", "y")

    @Test
    fun rejectsFlayerWhenLiftEndsOnY() {
        val score = HeatmapLiteralSwipeScore_v5.scoreWord(
            candidate = "flayer",
            pathLetters = pathFlayery,
            startLabel = "f",
            endLabel = "y",
        )
        assertEquals(Double.NEGATIVE_INFINITY, score, 0.0)
    }

    @Test
    fun scoresFlatteryWhenLiftEndsOnY() {
        val score = HeatmapLiteralSwipeScore_v5.scoreWord(
            candidate = "flattery",
            pathLetters = pathFlayery,
            startLabel = "f",
            endLabel = "y",
        )
        assertTrue(score > 0.0)
    }

    @Test
    fun flatteryBeatsFlayerWhenLiftEndsOnY() {
        val flattery = HeatmapLiteralSwipeScore_v5.scoreWord(
            candidate = "flattery",
            pathLetters = pathFlayery,
            startLabel = "f",
            endLabel = "y",
        )
        val flayer = HeatmapLiteralSwipeScore_v5.scoreWord(
            candidate = "flayer",
            pathLetters = pathFlayery,
            startLabel = "f",
            endLabel = "y",
        )
        assertTrue(flattery > flayer)
    }

    @Test
    fun stillRejectsIsOnImPath() {
        val score = HeatmapLiteralSwipeScore_v5.scoreWord(
            candidate = "is",
            pathLetters = listOf("i", "m"),
            startLabel = "i",
            endLabel = "m",
            strictEndMatch = true,
        )
        assertEquals(Double.NEGATIVE_INFINITY, score, 0.0)
    }
}
