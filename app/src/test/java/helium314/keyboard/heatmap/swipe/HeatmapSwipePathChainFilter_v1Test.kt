// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.heatmap.swipe

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class HeatmapSwipePathChainFilter_v1Test {

    private val graph = HeatmapKeyNeighborGraph_v2.staticQwerty()

    @Test
    fun dropsSpuriousAOnFetStroke() {
        val filtered = HeatmapSwipePathChainFilter_v1.filterSpuriousMiddleKeys(
            listOf("f", "a", "e", "t"),
            graph,
        )
        assertEquals(listOf("f", "e", "t"), filtered)
    }

    @Test
    fun keepsValidMiddleLetter() {
        val filtered = HeatmapSwipePathChainFilter_v1.filterSpuriousMiddleKeys(
            listOf("f", "e", "t"),
            graph,
        )
        assertEquals(listOf("f", "e", "t"), filtered)
    }
}
