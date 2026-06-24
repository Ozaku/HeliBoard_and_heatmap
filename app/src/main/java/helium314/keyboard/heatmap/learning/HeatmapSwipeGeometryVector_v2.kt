// SPDX-License-Identifier: GPL-3.0-only

// ai-note: v2 — exported swipe geometry that uses the SAME validated anchor detector as the live
// decoder (HeatmapGestureAnchors_v1: ~63-degree corners + real dwells + endpoints) instead of v1's
// legacy 16-degree corner detector, which over-detected corners and polluted the learning data
// (the "radio"/loose-letter noise the user flagged). Output reuses HeatmapSwipeGeometryVector_v1.Vector
// so every downstream consumer (WordSession.swipeGeometry, the export JSON) is unchanged.
// AI EDIT MAP:
//   build()        -> Vector with dwell segments (kinematics) + anchor-derived corner points/segments
//   cornerPoints() -> anchorIndices() mapped to PointSample (label + in-word role per finalWord)
package helium314.keyboard.heatmap.learning

import helium314.keyboard.heatmap.swipe.HeatmapGestureAnchors_v1
import helium314.keyboard.heatmap.swipe.HeatmapGestureKeyModel_v1
import helium314.keyboard.heatmap.swipe.HeatmapKeyLikelihood_v6
import helium314.keyboard.heatmap.swipe.HeatmapSwipeStrokeKinematics_v1
import helium314.keyboard.latin.common.InputPointers

object HeatmapSwipeGeometryVector_v2 {

    @JvmStatic
    fun build(
        pointers: InputPointers,
        layout: HeatmapCoordinateMap_v1.Snapshot,
        finalWord: String,
    ): HeatmapSwipeGeometryVector_v1.Vector? {
        val size = pointers.pointerSize
        if (size < 2) return null
        val kin = HeatmapSwipeStrokeKinematics_v1.analyze(layout, pointers)
        val keyModel = HeatmapGestureKeyModel_v1.from(layout)
        val corners = cornerPoints(pointers, keyModel, layout, finalWord)
        val merged = mergeSegments(
            dwellSegments = dwellSegmentSamples(kin, finalWord),
            cornerSegments = cornerSegmentSamples(corners),
        )
        return HeatmapSwipeGeometryVector_v1.Vector(
            layoutHash = layout.layoutHash,
            pointCount = size,
            segments = merged,
            cornerPoints = corners,
        )
    }

    private fun cornerPoints(
        pointers: InputPointers,
        keyModel: HeatmapGestureKeyModel_v1,
        layout: HeatmapCoordinateMap_v1.Snapshot,
        finalWord: String,
    ): List<HeatmapSwipeGeometryVector_v1.PointSample> {
        val size = pointers.pointerSize
        val xs = pointers.xCoordinates
        val ys = pointers.yCoordinates
        val times = pointers.times
        val points = ArrayList<DoubleArray>(size)
        val timesMs = IntArray(size)
        for (i in 0 until size) {
            points.add(doubleArrayOf(xs[i].toDouble(), ys[i].toDouble()))
            timesMs[i] = if (times != null && i < times.size) times[i] else i
        }
        val anchorIdx = HeatmapGestureAnchors_v1.anchorIndices(points, timesMs, keyModel)
        val out = ArrayList<HeatmapSwipeGeometryVector_v1.PointSample>(anchorIdx.size)
        for (idx in anchorIdx) {
            if (idx < 0 || idx >= size) continue
            val label = HeatmapKeyLikelihood_v6.bestLabelAt(layout, xs[idx], ys[idx])
            out.add(
                HeatmapSwipeGeometryVector_v1.PointSample(
                    index = idx,
                    x = xs[idx],
                    y = ys[idx],
                    label = label,
                    role = HeatmapSwipeKeyRoleClassifier_v1.roleForLabel(finalWord, label),
                    angleDeg = 0.0,
                ),
            )
        }
        return out
    }

    private fun dwellSegmentSamples(
        kin: HeatmapSwipeStrokeKinematics_v1.Result,
        finalWord: String,
    ): List<HeatmapSwipeGeometryVector_v1.SegmentSample> = kin.dwellSegments.map { dwell ->
        HeatmapSwipeGeometryVector_v1.SegmentSample(
            kind = HeatmapSwipeGeometryVector_v1.SegmentKind.DWELL,
            startIndex = dwell.startIndex,
            endIndex = dwell.endIndex,
            dominantLabel = dwell.dominantLabel,
            role = HeatmapSwipeKeyRoleClassifier_v1.roleForLabel(finalWord, dwell.dominantLabel),
            durationMs = dwell.durationMs,
            angleDeg = 0.0,
        )
    }

    private fun cornerSegmentSamples(
        corners: List<HeatmapSwipeGeometryVector_v1.PointSample>,
    ): List<HeatmapSwipeGeometryVector_v1.SegmentSample> = corners.map { corner ->
        HeatmapSwipeGeometryVector_v1.SegmentSample(
            kind = HeatmapSwipeGeometryVector_v1.SegmentKind.CORNER,
            startIndex = corner.index,
            endIndex = corner.index,
            dominantLabel = corner.label,
            role = corner.role,
            durationMs = 0L,
            angleDeg = corner.angleDeg,
        )
    }

    private fun mergeSegments(
        dwellSegments: List<HeatmapSwipeGeometryVector_v1.SegmentSample>,
        cornerSegments: List<HeatmapSwipeGeometryVector_v1.SegmentSample>,
    ): List<HeatmapSwipeGeometryVector_v1.SegmentSample> {
        val combined = (dwellSegments + cornerSegments)
            .sortedWith(compareBy({ it.startIndex }, { it.kind.ordinal }))
        if (combined.size <= HeatmapSwipeGeometryVector_v1.MAX_SEGMENTS) return combined
        return combined.take(HeatmapSwipeGeometryVector_v1.MAX_SEGMENTS)
    }
}
