// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.heatmap.swipe

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class HeatmapSwipeReachableLetters_v2Test {

    private val graph = HeatmapKeyNeighborGraph_v2.staticQwerty()
    private val pathFet = listOf("f", "e", "t")

    @Test
    fun rejectsFatOnFetPath() {
        assertFalse(
            HeatmapSwipeReachableLetters_v2.isCandidateReachable("fat", pathFet, graph, setOf('e')),
        )
    }

    @Test
    fun allowsFeetWithoutDwellHint() {
        assertTrue(
            HeatmapSwipeReachableLetters_v2.isCandidateReachable("feet", pathFet, graph, emptySet()),
        )
    }

    @Test
    fun rejectsFartOnFetPath() {
        assertFalse(
            HeatmapSwipeReachableLetters_v2.isCandidateReachable("fart", pathFet, graph, emptySet()),
        )
    }
}
