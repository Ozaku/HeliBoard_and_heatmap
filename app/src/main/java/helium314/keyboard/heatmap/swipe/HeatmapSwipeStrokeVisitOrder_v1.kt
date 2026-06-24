// SPDX-License-Identifier: GPL-3.0-only

// ai-note: Block 3 step 15q — full key visit order for path; no dominance-null gaps on F>L>Y>I>N>G

package helium314.keyboard.heatmap.swipe

import helium314.keyboard.heatmap.learning.HeatmapCoordinateMap_v1
import helium314.keyboard.latin.common.InputPointers

object HeatmapSwipeStrokeVisitOrder_v1 {

    @JvmStatic
    fun collect(
        layout: HeatmapCoordinateMap_v1.Snapshot,
        pointers: InputPointers,
    ): List<String> {
        val size = pointers.pointerSize
        if (size < 1) return emptyList()
        val xs = pointers.xCoordinates
        val ys = pointers.yCoordinates
        val out = ArrayList<String>(size / 4)
        var last: String? = null
        for (i in 0 until size) {
            val label = HeatmapKeyLikelihood_v6.bestLabelForPath(layout, xs[i], ys[i]) ?: continue
            if (label != last) {
                out.add(label)
                last = label
            }
        }
        return out
    }

    @JvmStatic
    fun labelsFromBeatPoints(
        layout: HeatmapCoordinateMap_v1.Snapshot,
        beatPoints: List<HeatmapSwipeBeat_v2.BeatPoint>,
    ): List<String> {
        if (beatPoints.isEmpty()) return emptyList()
        val out = ArrayList<String>(beatPoints.size)
        var last: String? = null
        for (beat in beatPoints) {
            val label = HeatmapKeyLikelihood_v6.bestLabelForPath(layout, beat.x, beat.y) ?: continue
            if (label != last) {
                out.add(label)
                last = label
            }
        }
        return out
    }
}
