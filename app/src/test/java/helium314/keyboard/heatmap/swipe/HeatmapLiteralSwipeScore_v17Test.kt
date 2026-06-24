// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.heatmap.swipe

import org.junit.Assert.assertEquals
import org.junit.Test

class HeatmapLiteralSwipeScore_v17Test {

    @Test
    fun exactOrderedPathScoresHigh() {
        val path = listOf("t", "e", "s", "t")
        val score = HeatmapLiteralSwipeScore_v17.scoreWord(
            candidate = "test",
            orderedPath = path,
            touchedLetters = setOf("t", "e", "s", "r"),
            startLabel = "t",
            startDistribution = listOf(HeatmapKeyLikelihood_v6.LabelWeight("t", 1.0)),
            endLabel = "t",
            requireEndMatch = true,
        )
        assert(score > 0.0)
    }

    @Test
    fun outOfOrderWordScoresNegativeInfinity() {
        val path = listOf("t", "e", "s", "t")
        val score = HeatmapLiteralSwipeScore_v17.scoreWord(
            candidate = "tset",
            orderedPath = path,
            touchedLetters = setOf("t", "e", "s"),
            startLabel = "t",
            startDistribution = listOf(HeatmapKeyLikelihood_v6.LabelWeight("t", 1.0)),
            endLabel = "t",
            requireEndMatch = true,
        )
        assertEquals(Double.NEGATIVE_INFINITY, score, 0.0)
    }
}