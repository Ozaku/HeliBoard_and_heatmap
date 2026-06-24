// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.heatmap.swipe

import org.junit.Assert.assertTrue
import org.junit.Test

class HeatmapSwipePathNeighborExpand_v1Test {

    @Test
    fun flayyRawExpandsTowardFlatteryPath() {
        val deduped = listOf("f", "l", "a", "y", "e", "r", "y")
        val raw = listOf("f", "l", "a", "y", "y", "e", "r", "y")
        val variants = HeatmapSwipePathNeighborExpand_v2.expand(deduped, raw, null, lightPreview = false)
        val joins = variants.map { it.letters.joinToString("") }
        assertTrue(joins.any { it.contains("flatt") || it == "flattery" })
    }
}
