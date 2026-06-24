// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.heatmap.swipe

import helium314.keyboard.latin.common.InputPointers
import org.junit.Assert.assertEquals
import org.junit.Test

class HeatmapSwipeStraightLine_v1Test {

    @Test
    fun collinearStrokeIsTwoLetterStraight() {
        val pts = linePointers(0, 0, 200, 40, 12)
        val analysis = HeatmapSwipeStraightLine_v1.analyze(pts)
        assertEquals(HeatmapSwipeStraightLine_v1.StrokeShape.NEAR_STRAIGHT_TWO_LETTER, analysis.shape)
        assertEquals(2, analysis.maxWordLength)
    }

    @Test
    fun zigZagIsGeneral() {
        val p = InputPointers(8)
        val xs = intArrayOf(0, 80, 40, 120, 200)
        val ys = intArrayOf(0, 0, 60, 0, 0)
        for (i in xs.indices) p.addPointer(xs[i], ys[i], i, 0)
        val analysis = HeatmapSwipeStraightLine_v1.analyze(p)
        assertEquals(HeatmapSwipeStraightLine_v1.StrokeShape.GENERAL, analysis.shape)
    }

    private fun linePointers(x0: Int, y0: Int, x1: Int, y1: Int, count: Int): InputPointers {
        val p = InputPointers(count)
        for (i in 0 until count) {
            val t = if (count <= 1) 0.0 else i.toDouble() / (count - 1)
            val x = (x0 + (x1 - x0) * t).toInt()
            val y = (y0 + (y1 - y0) * t).toInt()
            p.addPointer(x, y, i, 0)
        }
        return p
    }
}
