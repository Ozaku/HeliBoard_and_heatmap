// SPDX-License-Identifier: GPL-3.0-only

// ai-note: Phase1 step 1.1 — full stroke trace for export and offline baseline calibration

package helium314.keyboard.heatmap.swipe

import helium314.keyboard.heatmap.learning.HeatmapCoordinateMap_v1
import helium314.keyboard.latin.common.InputPointers
import kotlin.math.atan2
import kotlin.math.hypot

object HeatmapSwipeStrokeTrace_v1 {

    data class TracePoint(
        val x: Int,
        val y: Int,
        val tMs: Long,
        val bestLabel: String?,
        val topLabels: List<HeatmapKeyLikelihood_v6.LabelWeight>,
        val speedPxPerSec: Double,
        val segmentAngleDeg: Double,
    )

    data class Summary(
        val pointCount: Int,
        val durationMs: Long,
        val startDistribution: List<HeatmapKeyLikelihood_v6.LabelWeight>,
        val liftLabel: String?,
        val visitOrder: List<String>,
        val rejectedLetters: Set<String>,
    )

    data class Bundle(
        val points: List<TracePoint>,
        val summary: Summary,
    )

    @JvmStatic
    fun build(
        layout: HeatmapCoordinateMap_v1.Snapshot,
        pointers: InputPointers,
        touch: HeatmapSwipeStrokeTouchSet_v6.Result,
    ): Bundle {
        val cap = HeatmapSwipeTuningConstants_v1.EXPORT_TRACE_POINT_CAP
        val size = pointers.pointerSize
        if (size < 1) {
            return Bundle(emptyList(), Summary(0, 0L, emptyList(), null, emptyList(), emptySet()))
        }
        val xs = pointers.xCoordinates
        val ys = pointers.yCoordinates
        val times = pointers.times
        val step = if (size <= cap) 1 else (size + cap - 1) / cap
        val points = ArrayList<TracePoint>(cap)
        for (i in 0 until size step step) {
            val tMs = if (i == 0) 0L else (times[i] - times[0]).toLong()
            val speed = if (i > 0) {
                val dt = (times[i] - times[i - 1]).coerceAtLeast(1)
                val dist = hypot((xs[i] - xs[i - 1]).toDouble(), (ys[i] - ys[i - 1]).toDouble())
                dist * 1000.0 / dt
            } else {
                0.0
            }
            val angle = if (i + 1 < size) {
                Math.toDegrees(atan2((ys[i + 1] - ys[i]).toDouble(), (xs[i + 1] - xs[i]).toDouble()))
            } else {
                0.0
            }
            points.add(
                TracePoint(
                    x = xs[i],
                    y = ys[i],
                    tMs = tMs,
                    bestLabel = HeatmapKeyLikelihood_v6.bestLabelAt(layout, xs[i], ys[i]),
                    topLabels = HeatmapKeyLikelihood_v6.distributionAt(layout, xs[i], ys[i]),
                    speedPxPerSec = speed,
                    segmentAngleDeg = angle,
                ),
            )
        }
        val durationMs = (times[size - 1] - times[0]).toLong().coerceAtLeast(0L)
        return Bundle(
            points = points,
            summary = Summary(
                pointCount = size,
                durationMs = durationMs,
                startDistribution = touch.startDistribution,
                liftLabel = touch.liftLabel,
                visitOrder = touch.orderedLetters,
                rejectedLetters = touch.rejectedTouchLetters,
            ),
        )
    }
}
