// SPDX-License-Identifier: GPL-3.0-only

package helium314.keyboard.heatmap.swipe

import helium314.keyboard.heatmap.learning.HeatmapCoordinateMap_v1
import helium314.keyboard.latin.common.InputPointers
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class HeatmapSwipeStrokeKinematics_v1Test {

    private fun testLayout(): HeatmapCoordinateMap_v1.Snapshot =
        HeatmapCoordinateMap_v1.Snapshot(
            localeTag = "en",
            layoutSetExtra = "",
            mainLayoutName = "qwerty",
            elementId = 1,
            keyboardWidth = 1080,
            keyboardHeight = 600,
            layoutHash = "test",
            keys = listOf(
                HeatmapCoordinateMap_v1.KeyBoundsEntry("a", 102, 100, 100, 200, 200),
            ),
        )

    private fun testPointers(xs: IntArray, ys: IntArray, times: IntArray): InputPointers {
        val p = InputPointers(xs.size.coerceAtLeast(8))
        for (i in xs.indices) {
            p.addPointer(xs[i], ys[i], 0, times[i])
        }
        return p
    }

    @Test
    fun slowSegmentDetectedAsSlowStroke() {
        val pointers = testPointers(
            xs = intArrayOf(100, 105, 110, 115, 120),
            ys = intArrayOf(100, 100, 100, 100, 100),
            times = intArrayOf(0, 200, 400, 600, 800),
        )
        val layout = testLayout()
        val result = HeatmapSwipeStrokeKinematics_v1.analyze(layout, pointers)
        assertTrue(result.isSlowStroke)
        assertTrue(result.dwellSegments.isNotEmpty())
    }

    @Test
    fun fastSegmentIsNotSlowStroke() {
        val pointers = testPointers(
            xs = intArrayOf(100, 200, 300, 400, 500),
            ys = intArrayOf(100, 100, 100, 100, 100),
            times = intArrayOf(0, 50, 100, 150, 200),
        )
        val layout = testLayout()
        val result = HeatmapSwipeStrokeKinematics_v1.analyze(layout, pointers)
        assertFalse(result.isSlowStroke)
    }
}
