// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.heatmap.swipe

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class HeatmapPathLettersNormalize_v2Test {

    private val graph = HeatmapKeyNeighborGraph_v2.staticQwerty()

    @Test
    fun collapsesBridgeRBetweenET() {
        val result = HeatmapPathLettersNormalize_v2.collapseBridgeMiddleKeys(
            listOf("f", "e", "r", "t"),
            graph,
        )
        assertEquals(listOf("f", "e", "t"), result)
    }

    @Test
    fun detectsDwellOnE() {
        val raw = listOf("f", "e", "e", "e", "t")
        val deduped = listOf("f", "e", "t")
        val hints = HeatmapPathLettersNormalize_v2.detectDwellRuns(raw, deduped)
        assertTrue(hints.any { it.letter == "e" && it.rawRunLength >= 2 })
    }
}
