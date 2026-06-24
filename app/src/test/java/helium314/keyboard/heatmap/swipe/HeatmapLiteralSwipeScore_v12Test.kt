// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.heatmap.swipe

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class HeatmapLiteralSwipeScore_v12Test {

    private val pathFet = listOf("f", "e", "t")
    private val touchedFet = setOf("f", "e", "t")

    @Test
    fun rejectsFeeWhenLiftEndsOnT() {
        val score = HeatmapLiteralSwipeScore_v12.scoreWord(
            candidate = "fee",
            pathLetters = pathFet,
            touchedLetters = touchedFet,
            startLabel = "f",
            endLabel = "t",
            requireEndMatch = true,
        )
        assertEquals(Double.NEGATIVE_INFINITY, score, 0.0)
    }

    @Test
    fun allowsFeetWhenLiftEndsOnT() {
        val score = HeatmapLiteralSwipeScore_v12.scoreWord(
            candidate = "feet",
            pathLetters = pathFet,
            touchedLetters = touchedFet,
            startLabel = "f",
            endLabel = "t",
            requireEndMatch = true,
        )
        assertTrue(score > 0.7)
    }

    @Test
    fun reachableRejectsFeeWithoutConsumingT() {
        assertFalse(
            HeatmapSwipeReachableLetters_v5.isCandidateReachable("fee", pathFet, requirePathEnd = true),
        )
        assertTrue(
            HeatmapSwipeReachableLetters_v5.isCandidateReachable("feet", pathFet, requirePathEnd = true),
        )
    }
}
