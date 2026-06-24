// SPDX-License-Identifier: GPL-3.0-only

// ai-note: Block 3 step 15p — keep brief tail visits (g, n) on long swipes; v2 dropped them at 4% share

package helium314.keyboard.heatmap.swipe

import helium314.keyboard.heatmap.learning.HeatmapCoordinateMap_v1
import helium314.keyboard.latin.common.InputPointers

object HeatmapSwipeStrokeTouchSet_v3 {

    private const val MIN_SAMPLES_STRICT = 3
    private const val MIN_SHARE_STRICT = 0.04
    private const val MIN_SAMPLES_VISIT = 2

    data class Result(
        val touched: Set<String>,
        val counts: Map<String, Int>,
        val orderedLetters: List<String>,
        val rejectedTouchLetters: Set<String>,
        val startLabel: String?,
        val liftLabel: String?,
    )

    @JvmStatic
    fun collect(
        layout: HeatmapCoordinateMap_v1.Snapshot,
        pointers: InputPointers,
    ): Result {
        val counts = HashMap<String, Int>()
        val orderRaw = ArrayList<String>()
        val size = pointers.pointerSize
        if (size < 1) {
            return Result(emptySet(), emptyMap(), emptyList(), emptySet(), null, null)
        }
        val xs = pointers.xCoordinates
        val ys = pointers.yCoordinates
        var labeledTotal = 0
        for (i in 0 until size) {
            val label = HeatmapKeyLikelihood_v5.bestLabelAt(layout, xs[i], ys[i]) ?: continue
            labeledTotal++
            counts[label] = counts.getOrDefault(label, 0) + 1
            if (orderRaw.lastOrNull() != label) orderRaw.add(label)
        }
        val startLabel = HeatmapKeyLikelihood_v5.bestLabelAt(layout, xs[0], ys[0])
        val liftLabel = HeatmapSwipeLiftProject_v2.liftLabel(layout, pointers)
            ?: HeatmapKeyLikelihood_v5.bestLabelAt(layout, xs[size - 1], ys[size - 1])
        if (labeledTotal == 0) {
            return Result(emptySet(), counts, emptyList(), counts.keys, startLabel, liftLabel)
        }
        val visitSet = orderRaw.toSet()
        val touched = LinkedHashSet<String>()
        val rejected = LinkedHashSet<String>()
        for ((label, count) in counts) {
            val share = count.toDouble() / labeledTotal
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
        if (!startLabel.isNullOrEmpty()) touched.add(startLabel)
        if (!liftLabel.isNullOrEmpty()) touched.add(liftLabel)
        val orderedLetters = orderRaw.filter { it in touched }
        return Result(
            touched = touched,
            counts = counts,
            orderedLetters = orderedLetters,
            rejectedTouchLetters = rejected,
            startLabel = startLabel,
            liftLabel = liftLabel,
        )
    }

    @JvmStatic
    fun filterPathToTouched(path: List<String>, touched: Set<String>): List<String> =
        path.filter { it in touched }
}
