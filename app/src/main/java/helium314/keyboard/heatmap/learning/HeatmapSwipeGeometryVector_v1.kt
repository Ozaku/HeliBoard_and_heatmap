// SPDX-License-Identifier: GPL-3.0-only

// ai-note: compact swipe geometry vector from InputPointers + layout for correction-chain export

package helium314.keyboard.heatmap.learning

import helium314.keyboard.heatmap.swipe.HeatmapKeyLikelihood_v6
import helium314.keyboard.heatmap.swipe.HeatmapSwipeStrokeKinematics_v1
import helium314.keyboard.heatmap.swipe.HeatmapSwipeTuningConstants_v1
import helium314.keyboard.latin.common.InputPointers

object HeatmapSwipeGeometryVector_v1 {

    /** ai-note: role of a visited key relative to the kept final word */
    enum class KeyRole {
        FIRST,
        MIDDLE,
        LAST,
        IN_WORD,
        EXTRA,
        GAP,
    }

    enum class SegmentKind { DWELL, CORNER, TRANSIT }

    data class PointSample(
        val index: Int,
        val x: Int,
        val y: Int,
        val label: String?,
        val role: KeyRole,
        val angleDeg: Double,
    )

    data class SegmentSample(
        val kind: SegmentKind,
        val startIndex: Int,
        val endIndex: Int,
        val dominantLabel: String?,
        val role: KeyRole,
        val durationMs: Long,
        val angleDeg: Double,
    )

    data class Vector(
        val layoutHash: String,
        val pointCount: Int,
        val segments: List<SegmentSample>,
        val cornerPoints: List<PointSample>,
    )

    const val MAX_SEGMENTS = 10

    @JvmStatic
    fun build(
        pointers: InputPointers,
        layout: HeatmapCoordinateMap_v1.Snapshot,
        finalWord: String,
    ): Vector? {
        val size = pointers.pointerSize
        if (size < 2) return null
        val kin = HeatmapSwipeStrokeKinematics_v1.analyze(layout, pointers)
        val corners = cornerPoints(kin, pointers, layout, finalWord)
        val merged = mergeSegments(
            dwellSegments = dwellSegmentSamples(kin, finalWord),
            cornerSegments = cornerSegmentSamples(corners),
        )
        return Vector(
            layoutHash = layout.layoutHash,
            pointCount = size,
            segments = merged,
            cornerPoints = corners,
        )
    }

    private fun dwellSegmentSamples(
        kin: HeatmapSwipeStrokeKinematics_v1.Result,
        finalWord: String,
    ): List<SegmentSample> = kin.dwellSegments.map { dwell ->
        SegmentSample(
            kind = SegmentKind.DWELL,
            startIndex = dwell.startIndex,
            endIndex = dwell.endIndex,
            dominantLabel = dwell.dominantLabel,
            role = HeatmapSwipeKeyRoleClassifier_v1.roleForLabel(finalWord, dwell.dominantLabel),
            durationMs = dwell.durationMs,
            angleDeg = 0.0,
        )
    }

    private fun cornerPoints(
        kin: HeatmapSwipeStrokeKinematics_v1.Result,
        pointers: InputPointers,
        layout: HeatmapCoordinateMap_v1.Snapshot,
        finalWord: String,
    ): List<PointSample> {
        val cornerAngle = HeatmapSwipeTuningConstants_v1.CORNER_ANGLE_DEG
        val xs = pointers.xCoordinates
        val ys = pointers.yCoordinates
        val out = ArrayList<PointSample>()
        for (pk in kin.pointKinematics) {
            if (pk.segmentAngleDeg < cornerAngle) continue
            val idx = pk.index
            if (idx < 0 || idx >= pointers.pointerSize) continue
            val label = HeatmapKeyLikelihood_v6.bestLabelAt(layout, xs[idx], ys[idx])
            out.add(
                PointSample(
                    index = idx,
                    x = xs[idx],
                    y = ys[idx],
                    label = label,
                    role = HeatmapSwipeKeyRoleClassifier_v1.roleForLabel(finalWord, label),
                    angleDeg = pk.segmentAngleDeg,
                ),
            )
        }
        return out
    }

    /** ai-note: corner beats become segment rows for export; layout required for labels + roles */
    private fun cornerSegmentSamples(corners: List<PointSample>): List<SegmentSample> =
        corners.map { corner ->
            SegmentSample(
                kind = SegmentKind.CORNER,
                startIndex = corner.index,
                endIndex = corner.index,
                dominantLabel = corner.label,
                role = corner.role,
                durationMs = 0L,
                angleDeg = corner.angleDeg,
            )
        }

    private fun mergeSegments(
        dwellSegments: List<SegmentSample>,
        cornerSegments: List<SegmentSample>,
    ): List<SegmentSample> {
        val combined = (dwellSegments + cornerSegments)
            .sortedWith(compareBy({ it.startIndex }, { it.kind.ordinal }))
        if (combined.size <= MAX_SEGMENTS) return combined
        return combined.take(MAX_SEGMENTS)
    }
}
