// SPDX-License-Identifier: GPL-3.0-only

// ai-note: Block 3 step 15d — straight stroke via bearing stability (not chord deviation); caps word length

package helium314.keyboard.heatmap.swipe

import helium314.keyboard.latin.common.InputPointers
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.sqrt

object HeatmapSwipeStraightLine_v1 {

    enum class StrokeShape {
        GENERAL,
        NEAR_STRAIGHT_TWO_LETTER,
        NEAR_STRAIGHT_END_CURVE,
    }

    data class Analysis(
        val shape: StrokeShape,
        val maxWordLength: Int,
        val maxBearingChangeDeg: Double,
    ) {
        val locksLetterCount: Boolean = shape != StrokeShape.GENERAL
    }

    private const val MIN_SPAN_PX = 40
    private const val TWO_LETTER_MAX_BEARING_DEG = 22.0
    private const val END_CURVE_MAX_BEARING_DEG = 38.0
    private const val END_CURVE_TAIL_FRACTION = 0.22
    private const val MIN_SEGMENT_PX = 10

    @JvmStatic
    fun analyze(pointers: InputPointers): Analysis {
        val size = pointers.pointerSize
        if (size < 2) {
            return Analysis(StrokeShape.GENERAL, Int.MAX_VALUE, 180.0)
        }
        val xs = pointers.xCoordinates
        val ys = pointers.yCoordinates
        val span = distance(xs[0], ys[0], xs[size - 1], ys[size - 1])
        if (span < MIN_SPAN_PX) {
            return Analysis(StrokeShape.GENERAL, Int.MAX_VALUE, 180.0)
        }
        val bearings = bearingChangesDeg(xs, ys, size)
        if (bearings.isEmpty()) {
            return Analysis(StrokeShape.GENERAL, Int.MAX_VALUE, 180.0)
        }
        val overallMax = bearings.maxOrNull() ?: 180.0
        val tailStart = (bearings.size * (1.0 - END_CURVE_TAIL_FRACTION)).toInt()
            .coerceIn(0, bearings.lastIndex)
        val bodyMax = bearings.take(tailStart).maxOrNull() ?: 0.0
        val tailMax = bearings.drop(tailStart).maxOrNull() ?: 0.0
        val hasEndCurve = tailMax > bodyMax + 10.0 && tailMax > 14.0 && overallMax <= END_CURVE_MAX_BEARING_DEG
        return when {
            bodyMax <= TWO_LETTER_MAX_BEARING_DEG && hasEndCurve ->
                Analysis(StrokeShape.NEAR_STRAIGHT_END_CURVE, 3, overallMax)
            bodyMax <= TWO_LETTER_MAX_BEARING_DEG ->
                Analysis(StrokeShape.NEAR_STRAIGHT_TWO_LETTER, 2, overallMax)
            else -> Analysis(StrokeShape.GENERAL, Int.MAX_VALUE, overallMax)
        }
    }

    private fun bearingChangesDeg(xs: IntArray, ys: IntArray, size: Int): List<Double> {
        val dirs = ArrayList<Double>(size - 1)
        for (i in 0 until size - 1) {
            val dx = xs[i + 1] - xs[i]
            val dy = ys[i + 1] - ys[i]
            if (sqrt((dx * dx + dy * dy).toDouble()) < MIN_SEGMENT_PX) continue
            dirs.add(Math.toDegrees(atan2(dy.toDouble(), dx.toDouble())))
        }
        if (dirs.size < 2) return emptyList()
        val changes = ArrayList<Double>(dirs.size - 1)
        for (i in 0 until dirs.lastIndex) {
            changes.add(angleDeltaDeg(dirs[i], dirs[i + 1]))
        }
        return changes
    }

    private fun angleDeltaDeg(a: Double, b: Double): Double {
        var d = abs(b - a) % 360.0
        if (d > 180.0) d = 360.0 - d
        return d
    }

    private fun distance(x0: Int, y0: Int, x1: Int, y1: Int): Int {
        val dx = x1 - x0
        val dy = y1 - y0
        return sqrt((dx * dx + dy * dy).toDouble()).toInt()
    }
}
