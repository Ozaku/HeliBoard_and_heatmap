// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.heatmap.swipe

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class HeatmapLiteralSwipeScore_v4Test {

    private val pathIm = listOf("i", "m")

    @Test
    fun rejectsIsOnImPathWithStrictEnd() {
        val score = HeatmapLiteralSwipeScore_v4.scoreWord(
            candidate = "is",
            pathLetters = pathIm,
            startLabel = "i",
            endLabel = "m",
            strictEndMatch = true,
        )
        assertEquals(Double.NEGATIVE_INFINITY, score, 0.0)
    }

    @Test
    fun scoresImApostropheMOnImPath() {
        val score = HeatmapLiteralSwipeScore_v4.scoreWord(
            candidate = "I'm",
            pathLetters = pathIm,
            startLabel = "i",
            endLabel = "m",
            strictEndMatch = true,
        )
        assertTrue(score > 0.9)
    }
}
