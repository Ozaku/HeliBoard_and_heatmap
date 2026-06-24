// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.heatmap.swipe

import helium314.keyboard.latin.common.InputPointers
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class HeatmapSwipeBeat_v2Test {

    @Test
    fun zigzagStrokeProducesMoreBeatsThanStraightLine() {
        val straight = linePointers(0, 0, 240, 0, 24)
        val zigzag = eWigglePointers()
        val straightBeats = HeatmapSwipeBeat_v2.detect(straight).beatCount
        val zigzagBeats = HeatmapSwipeBeat_v2.detect(zigzag).beatCount
        assertTrue(straightBeats <= 3)
        assertTrue(zigzagBeats >= 5)
    }

    @Test
    fun sameKeyMicroCornersAddInteriorBeats() {
        val wiggle = eWigglePointers()
        val withoutLayout = HeatmapSwipeBeat_v2.detect(wiggle, layout = null).beatCount
        assertTrue(withoutLayout >= 5)
    }

    private fun linePointers(x0: Int, y0: Int, x1: Int, y1: Int, count: Int): InputPointers {
        val p = InputPointers(count)
        for (i in 0 until count) {
            val t = i.toDouble() / (count - 1).coerceAtLeast(1)
            p.addPointer(
                (x0 + (x1 - x0) * t).toInt(),
                (y0 + (y1 - y0) * t).toInt(),
                i,
                0,
            )
        }
        return p
    }

    /** ai-note: F→E wiggle→T polyline with repeated direction changes over E segment */
    private fun eWigglePointers(): InputPointers {
        val coords = listOf(
            0 to 0,
            40 to 0,
            80 to 0,
            100 to -8,
            110 to 8,
            120 to -10,
            130 to 10,
            140 to -8,
            150 to 8,
            160 to 0,
            200 to 0,
            240 to 0,
        )
        val p = InputPointers(coords.size)
        coords.forEachIndexed { i, (x, y) ->
            p.addPointer(x, y, i, 0)
        }
        return p
    }
}
