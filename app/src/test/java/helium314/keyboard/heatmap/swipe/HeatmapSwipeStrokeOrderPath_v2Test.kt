// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.heatmap.swipe

import org.junit.Assert.assertEquals
import org.junit.Test

class HeatmapSwipeStrokeOrderPath_v2Test {

    @Test
    fun allowsDoubleAtVisitedKey() {
        val filtered = HeatmapSwipeStrokeOrderPath_v2.filterToStrokeOrder(
            listOf("f", "e", "e", "t"),
            listOf("f", "e", "t"),
        )
        assertEquals(listOf("f", "e", "e", "t"), filtered)
    }

    @Test
    fun rejectsOutOfOrderInsert() {
        val filtered = HeatmapSwipeStrokeOrderPath_v2.filterToStrokeOrder(
            listOf("f", "a", "e", "t"),
            listOf("f", "e", "t"),
        )
        assertEquals(emptyList<String>(), filtered)
    }
}
