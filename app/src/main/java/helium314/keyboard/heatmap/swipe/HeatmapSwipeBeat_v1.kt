// SPDX-License-Identifier: GPL-3.0-only

// ai-note: Block 3 step 13b — sharp direction changes define swipe beat count (44_)

package helium314.keyboard.heatmap.swipe

import helium314.keyboard.latin.common.InputPointers
import kotlin.math.abs
import kotlin.math.atan2

object HeatmapSwipeBeat_v1 {

    private const val MIN_SEGMENT_PX = 12

    /** Minimum angle change (degrees) to count as a new beat / letter slot. */
    private const val CORNER_ANGLE_DEG = 38.0

    data class BeatPoint(val x: Int, val y: Int, val index: Int)

    data class Result(
        val beatCount: Int,
        val beatPoints: List<BeatPoint>,
    )

    fun detect(pointers: InputPointers): Result {
        val size = pointers.pointerSize
        if (size < 2) {
            return Result(beatCount = 0, beatPoints = emptyList())
        }
        val xs = pointers.xCoordinates
        val ys = pointers.yCoordinates
        val beats = ArrayList<BeatPoint>()
        beats.add(BeatPoint(xs[0], ys[0], 0))
        var prevDirDeg = directionDeg(xs[0], ys[0], xs[1], ys[1])
        var lastBeatX = xs[0]
        var lastBeatY = ys[0]
        for (i in 1 until size) {
            val x = xs[i]
            val y = ys[i]
            if (distance(lastBeatX, lastBeatY, x, y) < MIN_SEGMENT_PX) continue
            if (i + 1 < size) {
                val nextDir = directionDeg(x, y, xs[i + 1], ys[i + 1])
                if (angleDeltaDeg(prevDirDeg, nextDir) >= CORNER_ANGLE_DEG) {
                    beats.add(BeatPoint(x, y, i))
                    lastBeatX = x
                    lastBeatY = y
                    prevDirDeg = nextDir
                }
            }
        }
        val lastIdx = size - 1
        if (beats.last().index != lastIdx) {
            beats.add(BeatPoint(xs[lastIdx], ys[lastIdx], lastIdx))
        }
        return Result(beatCount = beats.size, beatPoints = beats)
    }

    private fun directionDeg(x0: Int, y0: Int, x1: Int, y1: Int): Double =
        Math.toDegrees(atan2((y1 - y0).toDouble(), (x1 - x0).toDouble()))

    private fun angleDeltaDeg(a: Double, b: Double): Double {
        var d = abs(b - a) % 360.0
        if (d > 180.0) d = 360.0 - d
        return d
    }

    private fun distance(x0: Int, y0: Int, x1: Int, y1: Int): Int {
        val dx = x1 - x0
        val dy = y1 - y0
        return kotlin.math.sqrt((dx * dx + dy * dy).toDouble()).toInt()
    }
}
