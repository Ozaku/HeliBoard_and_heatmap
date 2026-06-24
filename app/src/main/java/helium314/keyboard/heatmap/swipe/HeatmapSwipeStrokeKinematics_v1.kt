// SPDX-License-Identifier: GPL-3.0-only

// ai-note: Phase1 step 2.1 — per-segment speed/dwell/transit from InputPointers timestamps

package helium314.keyboard.heatmap.swipe

import helium314.keyboard.heatmap.learning.HeatmapCoordinateMap_v1
import helium314.keyboard.latin.common.InputPointers
import kotlin.math.atan2
import kotlin.math.hypot
import kotlin.math.abs

object HeatmapSwipeStrokeKinematics_v1 {

    enum class SegmentKind { DWELL, TRANSIT, CORNER, ENDPOINT }

    data class PointKinematics(
        val index: Int,
        val speedPxPerSec: Double,
        val segmentAngleDeg: Double,
    )

    data class DwellSegment(
        val startIndex: Int,
        val endIndex: Int,
        val startMs: Long,
        val endMs: Long,
        val durationMs: Long,
        val sampleCount: Int,
        val dominantLabel: String?,
    )

    data class Result(
        val pointKinematics: List<PointKinematics>,
        val dwellSegments: List<DwellSegment>,
        val avgSpeedKeyWidthsPerSec: Double,
        val durationMs: Long,
        val isSlowStroke: Boolean,
        val keyWidthPx: Double,
    )

    @JvmStatic
    fun analyze(
        layout: HeatmapCoordinateMap_v1.Snapshot,
        pointers: InputPointers,
    ): Result {
        val size = pointers.pointerSize
        val keyWidth = averageKeyWidth(layout).coerceAtLeast(1.0)
        if (size < 2) {
            return Result(emptyList(), emptyList(), 0.0, 0L, true, keyWidth)
        }
        val xs = pointers.xCoordinates
        val ys = pointers.yCoordinates
        val times = pointers.times
        val proto = HeatmapSwipeIntentPrototype_v1
        val dwellSpeedPx = proto.dwellSpeedKeyWidthsPerSec * keyWidth
        val transitSpeedPx = proto.transitSpeedKeyWidthsPerSec * keyWidth
        val slowAvg = proto.slowStrokeAvgKeyWidthsPerSec * keyWidth

        val pointKin = ArrayList<PointKinematics>(size)
        var speedSum = 0.0
        var speedCount = 0
        for (i in 1 until size) {
            val dt = (times[i] - times[i - 1]).coerceAtLeast(1)
            val dist = hypot(
                (xs[i] - xs[i - 1]).toDouble(),
                (ys[i] - ys[i - 1]).toDouble(),
            )
            val speed = dist * 1000.0 / dt
            val angle = if (i + 1 < size) {
                angleDeg(xs[i - 1], ys[i - 1], xs[i], ys[i])
            } else {
                0.0
            }
            pointKin.add(PointKinematics(i, speed, angle))
            speedSum += speed
            speedCount++
        }
        val avgSpeed = if (speedCount > 0) speedSum / speedCount else 0.0
        val durationMs = (times[size - 1] - times[0]).toLong().coerceAtLeast(0L)

        val dwells = detectDwells(
            layout, xs, ys, times, size, keyWidth, dwellSpeedPx, proto.dwellMinMs,
        )
        return Result(
            pointKinematics = pointKin,
            dwellSegments = dwells,
            avgSpeedKeyWidthsPerSec = avgSpeed / keyWidth,
            durationMs = durationMs,
            isSlowStroke = avgSpeed <= slowAvg,
            keyWidthPx = keyWidth,
        )
    }

    @JvmStatic
    fun segmentKindAt(
        kinematics: Result,
        index: Int,
        speedPxPerSec: Double,
    ): SegmentKind {
        val keyWidth = kinematics.keyWidthPx
        val dwellSpeed = HeatmapSwipeIntentPrototype_v1.dwellSpeedKeyWidthsPerSec * keyWidth
        val transitSpeed = HeatmapSwipeIntentPrototype_v1.transitSpeedKeyWidthsPerSec * keyWidth
        if (index == 0) return SegmentKind.ENDPOINT
        for (dwell in kinematics.dwellSegments) {
            if (index in dwell.startIndex..dwell.endIndex) return SegmentKind.DWELL
        }
        if (speedPxPerSec >= transitSpeed) return SegmentKind.TRANSIT
        if (speedPxPerSec <= dwellSpeed) return SegmentKind.DWELL
        return SegmentKind.TRANSIT
    }

    private fun detectDwells(
        layout: HeatmapCoordinateMap_v1.Snapshot,
        xs: IntArray,
        ys: IntArray,
        times: IntArray,
        size: Int,
        keyWidth: Double,
        dwellSpeedPx: Double,
        dwellMinMs: Long,
    ): List<DwellSegment> {
        val out = ArrayList<DwellSegment>()
        var runStart = 0
        var i = 1
        while (i < size) {
            val dt = (times[i] - times[i - 1]).coerceAtLeast(1)
            val dist = hypot((xs[i] - xs[i - 1]).toDouble(), (ys[i] - ys[i - 1]).toDouble())
            val speed = dist * 1000.0 / dt
            val inDwell = speed <= dwellSpeedPx
            if (!inDwell && i - runStart >= 2) {
                val seg = buildDwell(layout, xs, ys, times, runStart, i - 1, dwellMinMs)
                if (seg != null) out.add(seg)
                runStart = i
            }
            i++
        }
        if (size - runStart >= 2) {
            buildDwell(layout, xs, ys, times, runStart, size - 1, dwellMinMs)?.let { out.add(it) }
        }
        return out
    }

    private fun buildDwell(
        layout: HeatmapCoordinateMap_v1.Snapshot,
        xs: IntArray,
        ys: IntArray,
        times: IntArray,
        start: Int,
        end: Int,
        dwellMinMs: Long,
    ): DwellSegment? {
        val duration = (times[end] - times[start]).toLong()
        if (duration < dwellMinMs) return null
        val counts = HashMap<String, Int>()
        for (i in start..end) {
            val label = HeatmapKeyLikelihood_v6.bestLabelAt(layout, xs[i], ys[i]) ?: continue
            counts[label] = counts.getOrDefault(label, 0) + 1
        }
        val dominant = counts.maxByOrNull { it.value }?.key
        return DwellSegment(
            startIndex = start,
            endIndex = end,
            startMs = times[start].toLong(),
            endMs = times[end].toLong(),
            durationMs = duration,
            sampleCount = end - start + 1,
            dominantLabel = dominant,
        )
    }

    private fun averageKeyWidth(layout: HeatmapCoordinateMap_v1.Snapshot): Double {
        if (layout.keys.isEmpty()) return layout.keyboardWidth / 10.0
        return layout.keys.map { (it.right - it.left).coerceAtLeast(1) }.average()
    }

    private fun angleDeg(x0: Int, y0: Int, x1: Int, y1: Int): Double =
        Math.toDegrees(atan2((y1 - y0).toDouble(), (x1 - x0).toDouble()))

    private fun angleDeltaDeg(a: Double, b: Double): Double {
        var d = abs(b - a) % 360.0
        if (d > 180.0) d = 360.0 - d
        return d
    }
}
