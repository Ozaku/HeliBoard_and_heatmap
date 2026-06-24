// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.heatmap.swipe

import org.junit.Assert.assertTrue
import org.junit.Test

class HeatmapLiteralSwipeScore_v3Test {

    private val pathAcurate = listOf("a", "c", "u", "r", "a", "t", "e")

    @Test
    fun accurateScoresHigherThanArcuateOnAcuratePath() {
        val accurate = HeatmapLiteralSwipeScore_v3.scoreWord(
            candidate = "accurate",
            pathLetters = pathAcurate,
            startLabel = "a",
            endLabel = "e",
        )
        val arcuate = HeatmapLiteralSwipeScore_v3.scoreWord(
            candidate = "arcuate",
            pathLetters = pathAcurate,
            startLabel = "a",
            endLabel = "e",
        )
        assertTrue(accurate > arcuate)
        assertTrue(accurate > 0.9)
    }
}
