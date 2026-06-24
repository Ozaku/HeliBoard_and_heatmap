// SPDX-License-Identifier: GPL-3.0-only

// ai-note: Phase1 step 1.1 — touch set with v6 likelihood + start distribution for soft anchor

package helium314.keyboard.heatmap.swipe

import helium314.keyboard.heatmap.learning.HeatmapCoordinateMap_v1
import helium314.keyboard.latin.common.InputPointers

object HeatmapSwipeStrokeTouchSet_v6 {

    private const val MIN_SAMPLES_STRICT = 3
    private const val MIN_SHARE_STRICT = 0.04
    private const val MIN_SAMPLES_VISIT = 2

    data class Result(
        val touched: Set<String>,
        val counts: Map<String, Int>,
        val orderedLetters: List<String>,
        val rejectedTouchLetters: Set<String>,
        val startLabel: String?,
        val startDistribution: List<HeatmapKeyLikelihood_v6.LabelWeight>,
        val liftLabel: String?,
    )

    @JvmStatic
    fun collect(
        layout: HeatmapCoordinateMap_v1.Snapshot,
        pointers: InputPointers,
        kinematics: HeatmapSwipeStrokeKinematics_v1.Result? = null,
    ): Result {
        val counts = HashMap<String, Int>()
        val orderRaw = ArrayList<String>()
        val size = pointers.pointerSize
        if (size < 1) {
            return Result(emptySet(), emptyMap(), emptyList(), emptySet(), null, emptyList(), null)
        }
        val xs = pointers.xCoordinates
        val ys = pointers.yCoordinates
        var labeledTotal = 0
        for (i in 0 until size) {
            if (kinematics != null && i > 0) {
                val speed = kinematics.pointKinematics.firstOrNull { it.index == i }?.speedPxPerSec ?: 0.0
                val kind = HeatmapSwipeStrokeKinematics_v1.segmentKindAt(kinematics, i, speed)
                if (kind == HeatmapSwipeStrokeKinematics_v1.SegmentKind.TRANSIT) {
                    val transitLabel = HeatmapKeyLikelihood_v6.bestLabelAt(layout, xs[i], ys[i])
                    if (transitLabel != null) {
                        counts[transitLabel] = counts.getOrDefault(transitLabel, 0) + 1
                        if (orderRaw.lastOrNull() != transitLabel) orderRaw.add(transitLabel)
                    }
                    continue
                }
            }
            val label = HeatmapKeyLikelihood_v6.bestLabelAt(layout, xs[i], ys[i]) ?: continue
            labeledTotal++
            counts[label] = counts.getOrDefault(label, 0) + 1
            if (orderRaw.lastOrNull() != label) orderRaw.add(label)
        }
        val startDistribution = HeatmapKeyLikelihood_v6.distributionAt(layout, xs[0], ys[0], maxKeys = 3)
        val startLabel = HeatmapKeyLikelihood_v6.primaryStartLabel(startDistribution)
        val liftLabel = HeatmapSwipeLiftProject_v2.liftLabel(layout, pointers)
            ?: HeatmapKeyLikelihood_v6.bestLabelAt(layout, xs[size - 1], ys[size - 1])
        if (labeledTotal == 0) {
            return Result(
                touched = orderRaw.toSet(),
                counts = counts,
                orderedLetters = orderRaw,
                rejectedTouchLetters = counts.keys,
                startLabel = startLabel,
                startDistribution = startDistribution,
                liftLabel = liftLabel,
            )
        }
        val visitSet = orderRaw.toSet()
        val touched = LinkedHashSet<String>()
        val rejected = LinkedHashSet<String>()
        for ((label, count) in counts) {
            val share = count.toDouble() / labeledTotal.coerceAtLeast(1)
            val isVisit = label in visitSet
            val isEndpoint = label == startLabel || label == liftLabel
            val accepted = when {
                count >= MIN_SAMPLES_STRICT && share >= MIN_SHARE_STRICT -> true
                isVisit && count >= MIN_SAMPLES_VISIT -> true
                isEndpoint && count >= 1 -> true
                else -> false
            }
            if (accepted) touched.add(label) else rejected.add(label)
        }
        for (label in orderRaw) touched.add(label)
        if (!startLabel.isNullOrEmpty()) touched.add(startLabel)
        for (w in startDistribution) touched.add(w.label)
        if (!liftLabel.isNullOrEmpty()) touched.add(liftLabel)
        return Result(
            touched = touched,
            counts = counts,
            orderedLetters = orderRaw,
            rejectedTouchLetters = rejected,
            startLabel = startLabel,
            startDistribution = startDistribution,
            liftLabel = liftLabel,
        )
    }
}
