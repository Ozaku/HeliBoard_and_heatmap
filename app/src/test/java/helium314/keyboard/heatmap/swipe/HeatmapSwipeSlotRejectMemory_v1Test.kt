// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.heatmap.swipe

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class HeatmapSwipeSlotRejectMemory_v1Test {

    @Test
    fun remembersRejectedWordForSamePath() {
        HeatmapSwipeSlotRejectMemory_v1.clear()
        val path = listOf("t", "h", "u", "d")
        HeatmapSwipeSlotRejectMemory_v1.recordRejection(path, "t", "d", "thud")
        assertTrue(HeatmapSwipeSlotRejectMemory_v1.isRejected(path, "t", "d", "thud"))
        assertFalse(HeatmapSwipeSlotRejectMemory_v1.isRejected(path, "t", "d", "this"))
    }
}
