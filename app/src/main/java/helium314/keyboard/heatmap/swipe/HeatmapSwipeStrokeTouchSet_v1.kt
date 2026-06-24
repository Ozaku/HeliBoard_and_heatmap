// SPDX-License-Identifier: GPL-3.0-only

// ai-note: Block 3 step 15k — letters with ≥1 pointer sample inside v5 25% hit zone on full stroke

package helium314.keyboard.heatmap.swipe

import helium314.keyboard.heatmap.learning.HeatmapCoordinateMap_v1
import helium314.keyboard.latin.common.InputPointers

object HeatmapSwipeStrokeTouchSet_v1 {

    @JvmStatic
    fun collect(
        layout: HeatmapCoordinateMap_v1.Snapshot,
        pointers: InputPointers,
    ): Set<String> {
        val touched = LinkedHashSet<String>()
        val size = pointers.pointerSize
        if (size < 1) return touched
        val xs = pointers.xCoordinates
        val ys = pointers.yCoordinates
        for (i in 0 until size) {
            val label = HeatmapKeyLikelihood_v5.bestLabelAt(layout, xs[i], ys[i]) ?: continue
            touched.add(label)
        }
        return touched
    }

    @JvmStatic
    fun filterPathToTouched(
        path: List<String>,
        touched: Set<String>,
    ): List<String> = path.filter { it in touched }
}
