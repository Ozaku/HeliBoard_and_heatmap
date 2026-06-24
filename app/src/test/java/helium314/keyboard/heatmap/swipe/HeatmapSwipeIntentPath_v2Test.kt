// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.heatmap.swipe

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class HeatmapSwipeIntentPath_v2Test {
    @Test
    fun longFastSwipeExpandsShortIntentToVisitOrder() {
        val intent = HeatmapSwipeIntentClassifier_v2.Result(
            segments = emptyList(),
            visitOrder = listOf("p", "u", "r", "p", "o", "s", "e"),
            transitKeys = setOf("u", "r", "o"),
            intentLetters = listOf("p", "o", "s", "e"),
            startDistribution = emptyList(),
            liftLabel = "e",
        )
        val touch = HeatmapSwipeStrokeTouchSet_v6.Result(
            touched = setOf("p", "u", "r", "o", "s", "e"),
            counts = emptyMap(),
            orderedLetters = listOf("p", "u", "r", "p", "o", "s", "e"),
            rejectedTouchLetters = emptySet(),
            startLabel = "p",
            startDistribution = emptyList(),
            liftLabel = "e",
        )
        val kinematics = HeatmapSwipeStrokeKinematics_v1.Result(
            pointKinematics = emptyList(),
            dwellSegments = emptyList(),
            avgSpeedKeyWidthsPerSec = 4.0,
            durationMs = 400L,
            isSlowStroke = false,
            keyWidthPx = 100.0,
        )
        val path = HeatmapSwipeIntentPath_v2.build(
            intent = intent,
            touch = touch,
            cornerPath = intent.intentLetters,
            kinematics = kinematics,
            neighborGraph = HeatmapKeyNeighborGraph_v2.staticQwerty(),
        )
        assertTrue(path.size >= 6)
        assertEquals("purpose".take(path.size), path.joinToString(""))
    }

    @Test
    fun shortFourKeySwipeKeepsCornerIntent() {
        val intent = HeatmapSwipeIntentClassifier_v2.Result(
            segments = emptyList(),
            visitOrder = listOf("t", "h", "i", "s"),
            transitKeys = setOf("h"),
            intentLetters = listOf("t", "h", "s"),
            startDistribution = emptyList(),
            liftLabel = "s",
        )
        val touch = HeatmapSwipeStrokeTouchSet_v6.Result(
            touched = setOf("t", "h", "i", "s"),
            counts = emptyMap(),
            orderedLetters = listOf("t", "h", "i", "s"),
            rejectedTouchLetters = emptySet(),
            startLabel = "t",
            startDistribution = emptyList(),
            liftLabel = "s",
        )
        val kinematics = HeatmapSwipeStrokeKinematics_v1.Result(
            pointKinematics = emptyList(),
            dwellSegments = emptyList(),
            avgSpeedKeyWidthsPerSec = 3.5,
            durationMs = 250L,
            isSlowStroke = false,
            keyWidthPx = 100.0,
        )
        val path = HeatmapSwipeIntentPath_v2.build(
            intent = intent,
            touch = touch,
            cornerPath = intent.intentLetters,
            kinematics = kinematics,
            neighborGraph = HeatmapKeyNeighborGraph_v2.staticQwerty(),
        )
        assertEquals(3, path.size)
    }
}

