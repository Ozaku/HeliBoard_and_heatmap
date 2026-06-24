// SPDX-License-Identifier: GPL-3.0-only

// ai-note: v2 — keep adjacent duplicate letters; v1 distinct() dropped letters in longer words

package helium314.keyboard.heatmap.swipe

import helium314.keyboard.heatmap.learning.HeatmapCoordinateMap_v1
import helium314.keyboard.latin.common.InputPointers

object HeatmapSwipeIntentClassifier_v2 {

    data class ClassifiedSegment(
        val kind: HeatmapSwipeStrokeKinematics_v1.SegmentKind,
        val startIndex: Int,
        val endIndex: Int,
        val promotedLabel: String?,
        val transitLabels: List<String>,
    )

    data class Result(
        val segments: List<ClassifiedSegment>,
        val visitOrder: List<String>,
        val transitKeys: Set<String>,
        val intentLetters: List<String>,
        val startDistribution: List<HeatmapKeyLikelihood_v6.LabelWeight>,
        val liftLabel: String?,
    )

    @JvmStatic
    fun classify(
        layout: HeatmapCoordinateMap_v1.Snapshot,
        pointers: InputPointers,
        kinematics: HeatmapSwipeStrokeKinematics_v1.Result,
        rawBeats: HeatmapSwipeRawBeatInfer_v3.Result,
    ): Result {
        val size = pointers.pointerSize
        val xs = pointers.xCoordinates
        val ys = pointers.yCoordinates
        val visitOrder = ArrayList<String>()
        val transitKeys = LinkedHashSet<String>()
        val intentLetters = ArrayList<String>()
        val segments = ArrayList<ClassifiedSegment>()

        val startDist = if (size > 0) {
            HeatmapKeyLikelihood_v6.distributionAt(layout, xs[0], ys[0], maxKeys = 3)
        } else {
            emptyList()
        }
        val liftLabel = if (size > 0) {
            HeatmapSwipeLiftProject_v2.liftLabel(layout, pointers)
                ?: HeatmapKeyLikelihood_v6.bestLabelAt(layout, xs[size - 1], ys[size - 1])
        } else {
            null
        }

        for (dwell in kinematics.dwellSegments) {
            val label = dwell.dominantLabel
            if (label != null) {
                addVisit(visitOrder, label)
                if (label !in intentLetters || intentLetters.last() != label) {
                    intentLetters.add(label)
                }
            }
            segments.add(
                ClassifiedSegment(
                    kind = HeatmapSwipeStrokeKinematics_v1.SegmentKind.DWELL,
                    startIndex = dwell.startIndex,
                    endIndex = dwell.endIndex,
                    promotedLabel = label,
                    transitLabels = emptyList(),
                ),
            )
        }

        for (beat in rawBeats.classifiedBeats) {
            if (beat.kind == HeatmapGeometryClassifier_v2.BeatKind.BRIDGE) continue
            val label = HeatmapKeyLikelihood_v6.bestLabelAt(layout, beat.x, beat.y) ?: continue
            addVisit(visitOrder, label)
            if (beat.kind == HeatmapGeometryClassifier_v2.BeatKind.CORNER ||
                beat.kind == HeatmapGeometryClassifier_v2.BeatKind.START ||
                beat.kind == HeatmapGeometryClassifier_v2.BeatKind.END
            ) {
                if (intentLetters.isEmpty() || intentLetters.last() != label) {
                    intentLetters.add(label)
                }
                segments.add(
                    ClassifiedSegment(
                        kind = HeatmapSwipeStrokeKinematics_v1.SegmentKind.CORNER,
                        startIndex = beat.index,
                        endIndex = beat.index,
                        promotedLabel = label,
                        transitLabels = emptyList(),
                    ),
                )
            }
        }

        for (i in 0 until size) {
            val label = HeatmapKeyLikelihood_v6.bestLabelAt(layout, xs[i], ys[i]) ?: continue
            addVisit(visitOrder, label)
            val speed = kinematics.pointKinematics.firstOrNull { it.index == i }?.speedPxPerSec ?: 0.0
            val kind = HeatmapSwipeStrokeKinematics_v1.segmentKindAt(kinematics, i, speed)
            if (kind == HeatmapSwipeStrokeKinematics_v1.SegmentKind.TRANSIT) {
                transitKeys.add(label)
            }
        }

        val startPrimary = HeatmapKeyLikelihood_v6.primaryStartLabel(startDist)
        if (startPrimary != null && (intentLetters.isEmpty() || intentLetters.first() != startPrimary)) {
            if (intentLetters.isEmpty()) intentLetters.add(startPrimary)
            else intentLetters[0] = startPrimary
        }
        if (liftLabel != null && (intentLetters.isEmpty() || intentLetters.last() != liftLabel)) {
            intentLetters.add(liftLabel)
        }

        return Result(
            segments = segments,
            visitOrder = visitOrder,
            transitKeys = transitKeys,
            intentLetters = dedupeRuns(intentLetters),
            startDistribution = startDist,
            liftLabel = liftLabel,
        )
    }

    private fun addVisit(order: MutableList<String>, label: String) {
        if (order.lastOrNull() != label) order.add(label)
    }

    private fun dedupeRuns(labels: List<String>): List<String> {
        if (labels.isEmpty()) return labels
        val out = ArrayList<String>()
        for (l in labels) {
            if (out.isEmpty() || out.last() != l) out.add(l)
        }
        return out
    }
}

