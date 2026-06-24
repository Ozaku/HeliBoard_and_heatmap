// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.heatmap.swipe

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class HeatmapSwipeReachableLetters_v3Test {

    private val pathFet = listOf("f", "e", "t")

    @Test
    fun rejectsFatOnFetPath() {
        assertFalse(
            HeatmapSwipeReachableLetters_v3.isCandidateReachable("fat", pathFet, setOf('e')),
        )
    }

    @Test
    fun allowsFeetWithoutDwellHint() {
        assertTrue(
            HeatmapSwipeReachableLetters_v3.isCandidateReachable("feet", pathFet, emptySet()),
        )
    }

    @Test
    fun rejectsFitOnFetPath() {
        assertFalse(
            HeatmapSwipeReachableLetters_v3.isCandidateReachable("fit", pathFet, emptySet()),
        )
    }
}
