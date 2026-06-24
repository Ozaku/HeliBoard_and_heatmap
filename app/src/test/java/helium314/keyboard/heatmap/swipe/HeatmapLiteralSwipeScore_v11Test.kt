// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.heatmap.swipe

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class HeatmapLiteralSwipeScore_v11Test {

    @Test
    fun feetScoresOnFetPath() {
        val score = HeatmapLiteralSwipeScore_v11.scoreWord(
            candidate = "feet",
            pathLetters = listOf("f", "e", "t"),
            touchedLetters = setOf("f", "e", "t"),
            startLabel = "f",
            endLabel = "t",
        )
        assertTrue(score > 0.7)
    }

    @Test
    fun footScoresOnFotPath() {
        val score = HeatmapLiteralSwipeScore_v11.scoreWord(
            candidate = "foot",
            pathLetters = listOf("f", "o", "t"),
            touchedLetters = setOf("f", "o", "t"),
            startLabel = "f",
            endLabel = "t",
        )
        assertTrue(score > 0.7)
    }
}
