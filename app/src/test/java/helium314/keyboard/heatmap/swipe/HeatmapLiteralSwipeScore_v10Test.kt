// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.heatmap.swipe

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class HeatmapLiteralSwipeScore_v10Test {

    private val pathFet = listOf("f", "e", "t")
    private val touchedFet = setOf("f", "e", "t")
    private val dwellE = setOf('e')

    @Test
    fun rejectsFatWhenANotTouched() {
        val score = HeatmapLiteralSwipeScore_v10.scoreWord(
            candidate = "fat",
            pathLetters = pathFet,
            touchedLetters = touchedFet,
            startLabel = "f",
            endLabel = "t",
            dwellDoubleLetters = dwellE,
        )
        assertEquals(Double.NEGATIVE_INFINITY, score, 0.0)
    }

    @Test
    fun rejectsFartWhenANotTouched() {
        val score = HeatmapLiteralSwipeScore_v10.scoreWord(
            candidate = "fart",
            pathLetters = pathFet,
            touchedLetters = touchedFet,
            startLabel = "f",
            endLabel = "t",
            dwellDoubleLetters = emptySet(),
        )
        assertEquals(Double.NEGATIVE_INFINITY, score, 0.0)
    }

    @Test
    fun feetAllowedWhenETouched() {
        val score = HeatmapLiteralSwipeScore_v10.scoreWord(
            candidate = "feet",
            pathLetters = pathFet,
            touchedLetters = touchedFet,
            startLabel = "f",
            endLabel = "t",
            dwellDoubleLetters = dwellE,
        )
        assertTrue(score > 0.0)
    }
}
