// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.heatmap.swipe

import org.junit.Assert.assertTrue
import org.junit.Test

class HeatmapKeyNeighborGraph_v1Test {

    @Test
    fun tAndYAreQwertyNeighbors() {
        val graph = HeatmapKeyNeighborGraph_v2.staticQwerty()
        assertTrue(HeatmapKeyNeighborGraph_v1.areNeighbors(graph, "t", "y"))
        assertTrue(HeatmapKeyNeighborGraph_v1.areNeighbors(graph, "y", "t"))
    }
}
