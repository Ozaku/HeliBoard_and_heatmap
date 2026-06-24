// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.heatmap.swipe

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class HeatmapSwipeStrokeOrderPath_v1Test {

    @Test
    fun dropsOutOfOrderInsertedLetter() {
        val filtered = HeatmapSwipeStrokeOrderPath_v1.filterToStrokeOrder(
            listOf("f", "a", "e", "t"),
            listOf("f", "e", "t"),
        )
        assertTrue(filtered.isEmpty())
    }

    @Test
    fun keepsSubsequenceInStrokeOrder() {
        val filtered = HeatmapSwipeStrokeOrderPath_v1.filterToStrokeOrder(
            listOf("f", "e", "t"),
            listOf("f", "e", "t"),
        )
        assertTrue(filtered == listOf("f", "e", "t"))
    }
}
