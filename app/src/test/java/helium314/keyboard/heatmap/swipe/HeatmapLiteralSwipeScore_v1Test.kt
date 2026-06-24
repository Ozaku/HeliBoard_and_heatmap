// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.heatmap.swipe

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class HeatmapLiteralSwipeScore_v1Test {
    @Test
    fun startLetterMismatchRejected() {
        val score = HeatmapLiteralSwipeScore_v1.scoreWord(
            candidate = "bootlegger",
            pathLetters = listOf("c", "o", "m", "p", "a", "s"),
            startLabel = "c",
            endLabel = "s",
        )
        assertEquals(Double.NEGATIVE_INFINITY, score, 0.0)
    }

    @Test
    fun compassLikePathScoresHigh() {
        val score = HeatmapLiteralSwipeScore_v1.scoreWord(
            candidate = "compass",
            pathLetters = listOf("c", "o", "m", "p", "a", "s"),
            startLabel = "c",
            endLabel = "s",
        )
        assertTrue(score > 0.5)
    }

    @Test
    fun middleLettersWeightLowerThanFirst() {
        assertTrue(
            HeatmapLiteralSwipeScore_v1.letterWeight(0, 7) >
                HeatmapLiteralSwipeScore_v1.letterWeight(3, 7),
        )
    }
}
