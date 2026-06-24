// SPDX-License-Identifier: GPL-3.0-only

// ai-note: Block 3 step 15r — fewer false corners along swipe arcs; keep same-key wiggle pass

package helium314.keyboard.heatmap.swipe

import helium314.keyboard.latin.common.InputPointers
import kotlin.math.abs
import kotlin.math.atan2

object HeatmapSwipeBeat_v3 {

    private const val MIN_SEGMENT_PX = 10
    private const val CORNER_ANGLE_DEG = 18.0
    private const val SAME_KEY_CORNER_ANGLE_DEG = 12.0
    private const val SAME_KEY_MIN_SAMPLES = 4
    private const val BEAT_MERGE_PX = 8

    data class BeatPoint(val x: Int, val y: Int, val index: Int)

    data class Result(
        val beatCount: Int,
        val beatPoints: List<BeatPoint>,
    )

    fun detect(
        pointers: InputPointers,
        layout: helium314.keyboard.heatmap.learning.HeatmapCoordinateMap_v1.Snapshot? = null,
    ): Result {
        val size = pointers.pointerSize
        if (size < 2) return Result(0, emptyList())
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
        val merged = if (layout != null) mergeSameKeyCorners(layout, pointers, beats) else beats
        return Result(beatCount = merged.size, beatPoints = merged)
    }

    private fun mergeSameKeyCorners(
        layout: helium314.keyboard.heatmap.learning.HeatmapCoordinateMap_v1.Snapshot,
        pointers: InputPointers,
        beats: List<BeatPoint>,
    ): List<BeatPoint> {
        val size = pointers.pointerSize
        if (size < SAME_KEY_MIN_SAMPLES) return beats
        val xs = pointers.xCoordinates
        val ys = pointers.yCoordinates
        val extras = ArrayList<BeatPoint>()
        var i = 0
        while (i < size) {
            val label = HeatmapKeyLikelihood_v5.bestLabelAt(layout, xs[i], ys[i]) ?: run {
                i++
                continue
            }
            var run = 1
            while (i + run < size) {
                val next = HeatmapKeyLikelihood_v5.bestLabelAt(layout, xs[i + run], ys[i + run])
                if (next != label) break
                run++
            }
            if (run >= SAME_KEY_MIN_SAMPLES) {
                var prevDir: Double? = null
                var lastX = xs[i]
                var lastY = ys[i]
                for (k in 1 until run) {
                    val idx = i + k
                    val x = xs[idx]
                    val y = ys[idx]
                    if (distance(lastX, lastY, x, y) < 4) continue
                    val dir = directionDeg(lastX, lastY, x, y)
                    if (prevDir != null && angleDeltaDeg(prevDir, dir) >= SAME_KEY_CORNER_ANGLE_DEG) {
                        extras.add(BeatPoint(x, y, idx))
                    }
                    prevDir = dir
                    lastX = x
                    lastY = y
                }
            }
            i += run
        }
        if (extras.isEmpty()) return beats
        val all = (beats + extras).sortedBy { it.index }
        val out = ArrayList<BeatPoint>(all.size)
        for (point in all) {
            val dup = out.lastOrNull()
            if (dup != null && dup.index == point.index) continue
            if (dup != null &&
                distance(dup.x, dup.y, point.x, point.y) < BEAT_MERGE_PX &&
                kotlin.math.abs(dup.index - point.index) <= 2
            ) {
                continue
            }
            out.add(point)
        }
        return out
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
