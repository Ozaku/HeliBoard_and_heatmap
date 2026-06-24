// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.heatmap.swipe

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class HeatmapLiteralSwipeScore_v6Test {

    private val pathFlayery = listOf("f", "l", "a", "y", "e", "r", "y")
    private val graph = HeatmapKeyNeighborGraph_v2.staticQwerty()

    @Test
    fun rejectsFlayerWhenLiftEndsOnY() {
        val score = HeatmapLiteralSwipeScore_v6.scoreWord(
            candidate = "flayer",
            pathLetters = pathFlayery,
            startLabel = "f",
            endLabel = "y",
            neighborGraph = graph,
        )
        assertEquals(Double.NEGATIVE_INFINITY, score, 0.0)
    }

    @Test
    fun flatteryScoresWithMissedTNeighborY() {
        val score = HeatmapLiteralSwipeScore_v6.scoreWord(
            candidate = "flattery",
            pathLetters = pathFlayery,
            startLabel = "f",
            endLabel = "y",
            neighborGraph = graph,
        )
        assertTrue(score >= 0.75)
    }

    @Test
    fun flatteryBeatsFlayerOnNeighborPath() {
        val flattery = HeatmapLiteralSwipeScore_v6.scoreWord(
            candidate = "flattery",
            pathLetters = pathFlayery,
            startLabel = "f",
            endLabel = "y",
            neighborGraph = graph,
        )
        val flayer = HeatmapLiteralSwipeScore_v6.scoreWord(
            candidate = "flayer",
            pathLetters = pathFlayery,
            startLabel = "f",
            endLabel = "y",
            neighborGraph = graph,
        )
        assertTrue(flattery > flayer)
    }

    @Test
    fun neighborAllowsMissedMiddleKey() {
        val pathFlay = listOf("f", "l", "a", "y")
        val flat = HeatmapLiteralSwipeScore_v6.scoreWord(
            candidate = "flat",
            pathLetters = pathFlay,
            startLabel = "f",
            endLabel = "t",
            neighborGraph = graph,
        )
        assertTrue(flat > 0.0)
    }
}
