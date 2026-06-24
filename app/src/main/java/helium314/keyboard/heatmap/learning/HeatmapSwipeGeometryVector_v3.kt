// SPDX-License-Identifier: GPL-3.0-only

// ai-note: v3 — exported swipe geometry that uses HeatmapGestureAnchors_v2, the SAME anchor
// detector the live v2 decoder uses for candidate generation. v2 (this file's predecessor) used
// HeatmapGestureAnchors_v1, whose time-based dwell detector never fired on real traces
// (swipeDwellSegments was always []), so brief HARD STOPS were invisible in the learning data.
// v2-anchors add velocity-minima stop detection, so a stop is always captured. Output still
// reuses HeatmapSwipeGeometryVector_v1.Vector so every downstream consumer is unchanged:
//   - cornerPoints  = every anchor (START/STOP/CORNER/END) -> nearest key, angleDeg = turn degrees
//   - segments      = STOP anchors as DWELL kind, CORNER anchors as CORNER kind (type is recoverable)
// The full raw x,y,t,speed trace (now 512-cap) is the ground truth for tuning anchor thresholds.
// AI EDIT MAP:
//   build()       -> Vector(anchors-as-cornerPoints + stop/corner segments)
//   anchorPoints()-> HeatmapGestureAnchors_v2.extract -> PointSample list
package helium314.keyboard.heatmap.learning

import helium314.keyboard.heatmap.swipe.HeatmapGestureAnchors_v2
import helium314.keyboard.heatmap.swipe.HeatmapGestureKeyModel_v1
import helium314.keyboard.latin.common.InputPointers

object HeatmapSwipeGeometryVector_v3 {

    @JvmStatic
    fun build(
        pointers: InputPointers,
        layout: HeatmapCoordinateMap_v1.Snapshot,
        finalWord: String,
    ): HeatmapSwipeGeometryVector_v1.Vector? {
        val size = pointers.pointerSize
        if (size < 2) return null
        val keyModel = HeatmapGestureKeyModel_v1.from(layout)
        val anchors = anchorPoints(pointers, keyModel, finalWord)
        val merged = mergeSegments(anchorSegmentSamples(anchors))
        return HeatmapSwipeGeometryVector_v1.Vector(
            layoutHash = layout.layoutHash,
            pointCount = size,
            segments = merged,
            cornerPoints = anchors.map { it.sample },
        )
    }

    private class AnchorRow(
        val sample: HeatmapSwipeGeometryVector_v1.PointSample,
        val type: HeatmapGestureAnchors_v2.Type,
    )

    private fun anchorPoints(
        pointers: InputPointers,
        keyModel: HeatmapGestureKeyModel_v1,
        finalWord: String,
    ): List<AnchorRow> {
        val size = pointers.pointerSize
        val xs = pointers.xCoordinates
        val ys = pointers.yCoordinates
        val times = pointers.times
        val points = ArrayList<DoubleArray>(size)
        val timesMs = IntArray(size)
        for (i in 0 until size) {
            points.add(doubleArrayOf(xs[i].toDouble(), ys[i].toDouble()))
            timesMs[i] = if (i < times.size) times[i] else i
        }
        val extracted = HeatmapGestureAnchors_v2.extract(points, timesMs, keyModel)
        val out = ArrayList<AnchorRow>(extracted.anchors.size)
        for (a in extracted.anchors) {
            if (a.index < 0 || a.index >= size) continue
            val label = a.key?.toString()
            out.add(
                AnchorRow(
                    sample = HeatmapSwipeGeometryVector_v1.PointSample(
                        index = a.index,
                        x = xs[a.index],
                        y = ys[a.index],
                        label = label,
                        role = HeatmapSwipeKeyRoleClassifier_v1.roleForLabel(finalWord, label),
                        angleDeg = Math.toDegrees(a.turnAngleRad),
                    ),
                    type = a.type,
                ),
            )
        }
        return out
    }

    // STOP -> DWELL segment kind, CORNER -> CORNER; endpoints are not emitted as segments (they are
    // already present in cornerPoints and implied by the path ends).
    private fun anchorSegmentSamples(
        anchors: List<AnchorRow>,
    ): List<HeatmapSwipeGeometryVector_v1.SegmentSample> {
        val out = ArrayList<HeatmapSwipeGeometryVector_v1.SegmentSample>(anchors.size)
        for (row in anchors) {
            val kind = when (row.type) {
                HeatmapGestureAnchors_v2.Type.STOP -> HeatmapSwipeGeometryVector_v1.SegmentKind.DWELL
                HeatmapGestureAnchors_v2.Type.CORNER -> HeatmapSwipeGeometryVector_v1.SegmentKind.CORNER
                else -> continue
            }
            out.add(
                HeatmapSwipeGeometryVector_v1.SegmentSample(
                    kind = kind,
                    startIndex = row.sample.index,
                    endIndex = row.sample.index,
                    dominantLabel = row.sample.label,
                    role = row.sample.role,
                    durationMs = 0L,
                    angleDeg = row.sample.angleDeg,
                ),
            )
        }
        return out
    }

    private fun mergeSegments(
        segments: List<HeatmapSwipeGeometryVector_v1.SegmentSample>,
    ): List<HeatmapSwipeGeometryVector_v1.SegmentSample> {
        val sorted = segments.sortedWith(compareBy({ it.startIndex }, { it.kind.ordinal }))
        if (sorted.size <= HeatmapSwipeGeometryVector_v1.MAX_SEGMENTS) return sorted
        return sorted.take(HeatmapSwipeGeometryVector_v1.MAX_SEGMENTS)
    }
}
