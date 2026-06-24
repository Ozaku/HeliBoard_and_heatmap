// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.heatmap.swipe

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class HeatmapKeyNeighborGraph_v2Test {

    @Test
    fun fAndANotNeighbors() {
        val graph = HeatmapKeyNeighborGraph_v2.staticQwerty()
        assertFalse(HeatmapKeyNeighborGraph_v2.areNeighbors(graph, "f", "a"))
    }

    @Test
    fun eAndRAreNeighbors() {
        val graph = HeatmapKeyNeighborGraph_v2.staticQwerty()
        assertTrue(HeatmapKeyNeighborGraph_v2.areNeighbors(graph, "e", "r"))
    }
}
