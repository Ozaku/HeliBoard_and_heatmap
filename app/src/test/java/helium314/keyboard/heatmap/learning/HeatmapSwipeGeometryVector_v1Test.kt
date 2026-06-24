// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.heatmap.learning

import helium314.keyboard.latin.common.InputPointers
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class HeatmapSwipeGeometryVector_v1Test {

    private fun qwertyLayout(): HeatmapCoordinateMap_v1.Snapshot =
        HeatmapCoordinateMap_v1.Snapshot(
            localeTag = "en",
            layoutSetExtra = "",
            mainLayoutName = "qwerty",
            elementId = 1,
            keyboardWidth = 1080,
            keyboardHeight = 600,
            layoutHash = "lh_test",
            keys = listOf(
                HeatmapCoordinateMap_v1.KeyBoundsEntry("h", 104, 100, 100, 200, 200),
                HeatmapCoordinateMap_v1.KeyBoundsEntry("e", 101, 220, 100, 320, 200),
                HeatmapCoordinateMap_v1.KeyBoundsEntry("l", 108, 340, 100, 440, 200),
                HeatmapCoordinateMap_v1.KeyBoundsEntry("o", 111, 460, 100, 560, 200),
            ),
        )

    private fun pointers(xs: IntArray, ys: IntArray, times: IntArray): InputPointers {
        val p = InputPointers(xs.size.coerceAtLeast(8))
        for (i in xs.indices) {
            p.addPointer(xs[i], ys[i], 0, times[i])
        }
        return p
    }

    @Test
    fun buildReturnsVectorWithSegmentsWhenPointersPresent() {
        val layout = qwertyLayout()
        val p = pointers(
            xs = intArrayOf(150, 260, 380, 500),
            ys = intArrayOf(150, 150, 150, 150),
            times = intArrayOf(0, 120, 240, 360),
        )
        val vector = HeatmapSwipeGeometryVector_v1.build(p, layout, "hello")
        assertNotNull(vector)
        assertEquals("lh_test", vector!!.layoutHash)
        assertEquals(4, vector.pointCount)
        assertTrue(vector.segments.size <= HeatmapSwipeGeometryVector_v1.MAX_SEGMENTS)
    }

    @Test
    fun cornerPointsUseLayoutForLabelsAndRoles() {
        val layout = qwertyLayout()
        val p = pointers(
            xs = intArrayOf(150, 150, 260, 380, 500),
            ys = intArrayOf(150, 120, 150, 150, 150),
            times = intArrayOf(0, 80, 160, 240, 320),
        )
        val vector = HeatmapSwipeGeometryVector_v1.build(p, layout, "hello")
        assertNotNull(vector)
        if (vector!!.cornerPoints.isNotEmpty()) {
            val corner = vector.cornerPoints.first()
            assertEquals(HeatmapSwipeGeometryVector_v1.KeyRole.FIRST, corner.role)
            assertEquals("h", corner.label)
        }
    }

    @Test
    fun needsAtLeastTwoPoints() {
        val layout = qwertyLayout()
        val p = pointers(intArrayOf(150), intArrayOf(150), intArrayOf(0))
        assertEquals(null, HeatmapSwipeGeometryVector_v1.build(p, layout, "h"))
    }
}
