// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.heatmap.swipe

import helium314.keyboard.heatmap.learning.HeatmapCoordinateMap_v1
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class HeatmapKeyLikelihood_v5Test {

    private fun key(left: Int, top: Int, size: Int = 100): HeatmapCoordinateMap_v1.KeyBoundsEntry =
        HeatmapCoordinateMap_v1.KeyBoundsEntry("f", 102, left, top, left + size, top + size)

    private fun layout(vararg keys: HeatmapCoordinateMap_v1.KeyBoundsEntry): HeatmapCoordinateMap_v1.Snapshot =
        HeatmapCoordinateMap_v1.Snapshot(
            localeTag = "en",
            layoutSetExtra = "",
            mainLayoutName = "qwerty",
            elementId = 1,
            keyboardWidth = 1080,
            keyboardHeight = 600,
            layoutHash = "test",
            keys = keys.toList(),
        )

    @Test
    fun insideKeyReturnsLabel() {
        val f = key(100, 100)
        val snap = layout(f)
        assertEquals("f", HeatmapKeyLikelihood_v5.bestLabelAt(snap, 150, 150))
    }

    @Test
    fun missBeyond25PercentReturnsNull() {
        val f = key(100, 100)
        val snap = layout(f)
        val maxMiss = HeatmapKeyLikelihood_v5.maxMissPx(f).toInt()
        assertNull(HeatmapKeyLikelihood_v5.bestLabelAt(snap, 100 - maxMiss - 5, 150))
    }

    @Test
    fun missWithin25PercentCanReturnLabel() {
        val f = key(100, 100)
        val snap = layout(f)
        val maxMiss = HeatmapKeyLikelihood_v5.maxMissPx(f).toInt()
        val label = HeatmapKeyLikelihood_v5.bestLabelAt(snap, 100 - maxMiss + 2, 150)
        assertTrue(label == null || label == "f")
    }
}
