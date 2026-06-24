// SPDX-License-Identifier: GPL-3.0-only

// ai-note: Block 3 step 15l — touched letter needs ≥3 samples AND ≥4% of stroke; kills single-point A noise

package helium314.keyboard.heatmap.swipe

import helium314.keyboard.heatmap.learning.HeatmapCoordinateMap_v1
import helium314.keyboard.latin.common.InputPointers

object HeatmapSwipeStrokeTouchSet_v2 {

    private const val MIN_SAMPLES = 3
    private const val MIN_SHARE = 0.04

    data class Result(
        val touched: Set<String>,
        val counts: Map<String, Int>,
        val orderedLetters: List<String>,
        val rejectedLowCount: Set<String>,
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
            return Result(emptySet(), emptyMap(), emptyList(), emptySet())
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
        if (labeledTotal == 0) {
            return Result(emptySet(), counts, emptyList(), counts.keys)
        }
        val touched = LinkedHashSet<String>()
        val rejected = LinkedHashSet<String>()
        for ((label, count) in counts) {
            val share = count.toDouble() / labeledTotal
            if (count >= MIN_SAMPLES && share >= MIN_SHARE) {
                touched.add(label)
            } else {
                rejected.add(label)
            }
        }
        val orderedLetters = orderRaw.filter { it in touched }
        return Result(
            touched = touched,
            counts = counts,
            orderedLetters = orderedLetters,
            rejectedLowCount = rejected,
        )
    }

    @JvmStatic
    fun filterPathToTouched(path: List<String>, touched: Set<String>): List<String> =
        path.filter { it in touched }
}
