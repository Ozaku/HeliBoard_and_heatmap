// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.heatmap.swipe

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class HeatmapLiteralSwipeScore_v7Test {

    private val pathFet = listOf("f", "e", "t")
    private val graph = HeatmapKeyNeighborGraph_v2.staticQwerty()
    private val dwellE = setOf('e')

    @Test
    fun rejectsFatOnFet() {
        val score = HeatmapLiteralSwipeScore_v7.scoreWord(
            candidate = "fat",
            pathLetters = pathFet,
            startLabel = "f",
            endLabel = "t",
            neighborGraph = graph,
            dwellDoubleLetters = dwellE,
        )
        assertEquals(Double.NEGATIVE_INFINITY, score, 0.0)
    }

    @Test
    fun feetBeatsFitWithDwellOnE() {
        val feet = HeatmapLiteralSwipeScore_v7.scoreWord(
            candidate = "feet",
            pathLetters = pathFet,
            startLabel = "f",
            endLabel = "t",
            neighborGraph = graph,
            dwellDoubleLetters = dwellE,
        )
        val fit = HeatmapLiteralSwipeScore_v7.scoreWord(
            candidate = "fit",
            pathLetters = pathFet,
            startLabel = "f",
            endLabel = "t",
            neighborGraph = graph,
            dwellDoubleLetters = dwellE,
        )
        assertTrue(feet > fit)
    }
}
