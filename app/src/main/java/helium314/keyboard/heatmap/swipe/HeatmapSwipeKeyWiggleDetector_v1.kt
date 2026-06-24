// SPDX-License-Identifier: GPL-3.0-only

// ai-note: Block 3 step 15o — zig-zag angle count while finger stays on one key → double letter hint

package helium314.keyboard.heatmap.swipe

import helium314.keyboard.heatmap.learning.HeatmapCoordinateMap_v1
import helium314.keyboard.latin.common.InputPointers
import kotlin.math.abs
import kotlin.math.atan2

object HeatmapSwipeKeyWiggleDetector_v1 {

    private const val MIN_SAMPLES_ON_KEY = 4
    private const val MIN_ZIGZAG_ANGLE_DEG = 10.0
    private const val MIN_ZIGZAG_COUNT = 1
    private const val MIN_SEGMENT_PX = 4

    data class Hint(
        val letter: String,
        val pathIndex: Int,
        val zigzagCount: Int,
    )

    @JvmStatic
    fun detect(
        layout: HeatmapCoordinateMap_v1.Snapshot,
        pointers: InputPointers,
        pathLetters: List<String>,
    ): List<Hint> {
        if (pathLetters.size < 2 || pointers.pointerSize < MIN_SAMPLES_ON_KEY) return emptyList()
        val size = pointers.pointerSize
        val xs = pointers.xCoordinates
        val ys = pointers.yCoordinates
        val samples = ArrayList<Pair<String, Int>>(size)
        for (i in 0 until size) {
            val label = HeatmapKeyLikelihood_v5.bestLabelAt(layout, xs[i], ys[i]) ?: continue
            samples.add(label to i)
        }
        if (samples.size < MIN_SAMPLES_ON_KEY) return emptyList()
        val hints = ArrayList<Hint>()
        var i = 0
        while (i < samples.size) {
            val letter = samples[i].first
            var run = 1
            while (i + run < samples.size && samples[i + run].first == letter) run++
            if (run >= MIN_SAMPLES_ON_KEY && letter in pathLetters) {
                val zigzags = countZigzags(xs, ys, samples.subList(i, i + run).map { it.second })
                val pathIdx = pathLetters.indexOf(letter)
                if (zigzags >= MIN_ZIGZAG_COUNT && pathIdx in 1 until pathLetters.lastIndex) {
                    hints.add(Hint(letter = letter, pathIndex = pathIdx, zigzagCount = zigzags))
                }
            }
            i += run
        }
        return hints
    }

    @JvmStatic
    fun expandPathDoubles(
        path: List<String>,
        hints: List<Hint>,
    ): List<String> {
        if (hints.isEmpty()) return path
        val byIndex = hints.associateBy { it.pathIndex }
        val out = ArrayList<String>(path.size + hints.size)
        path.forEachIndexed { idx, letter ->
            out.add(letter)
            if (byIndex.containsKey(idx)) out.add(letter)
        }
        return capTripleRuns(out)
    }

    private fun countZigzags(xs: IntArray, ys: IntArray, indices: List<Int>): Int {
        if (indices.size < 4) return 0
        var zigzags = 0
        var prevDir: Double? = null
        var lastX = xs[indices[0]]
        var lastY = ys[indices[0]]
        for (k in 1 until indices.size) {
            val idx = indices[k]
            val x = xs[idx]
            val y = ys[idx]
            if (distance(lastX, lastY, x, y) < MIN_SEGMENT_PX) continue
            val dir = directionDeg(lastX, lastY, x, y)
            if (prevDir != null && angleDeltaDeg(prevDir, dir) >= MIN_ZIGZAG_ANGLE_DEG) {
                zigzags++
            }
            prevDir = dir
            lastX = x
            lastY = y
        }
        return zigzags
    }

    private fun capTripleRuns(path: List<String>): List<String> {
        if (path.size < 3) return path
        val out = ArrayList<String>()
        var run = 0
        for (letter in path) {
            if (out.isNotEmpty() && out.last() == letter) {
                run++
                if (run >= 2) continue
            } else {
                run = 0
            }
            out.add(letter)
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
