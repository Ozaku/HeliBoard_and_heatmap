// SPDX-License-Identifier: GPL-3.0-only

// ai-note: Block 3 step 15q — multi-corner strokes skip bridge collapse; longest visit/beat path wins

package helium314.keyboard.heatmap.swipe

import helium314.keyboard.heatmap.learning.HeatmapCoordinateMap_v1
import helium314.keyboard.latin.common.InputPointers

object HeatmapPathLettersNormalize_v9 {

    private const val MULTI_CORNER_BEAT_MIN = 5
    private const val MULTI_CORNER_PATH_MIN = 4

    @JvmStatic
    fun normalize(
        rawBeatPath: List<String>,
        rawBeats: HeatmapSwipeBeat_v2.Result,
        neighborGraph: HeatmapKeyNeighborGraph_v2.Graph?,
        layout: HeatmapCoordinateMap_v1.Snapshot,
        pointers: InputPointers,
        touch: HeatmapSwipeStrokeTouchSet_v4.Result,
    ): HeatmapPathLettersNormalize_v2.Normalized {
        val visitOrder = touch.orderedLetters
        val allBeatPath = HeatmapSwipeStrokeVisitOrder_v1.labelsFromBeatPoints(
            layout, rawBeats.beatPoints,
        )
        val multiCorner = isMultiCornerStroke(rawBeats.beatCount, allBeatPath, visitOrder)
        if (multiCorner) {
            val path = pickLongestPath(visitOrder, allBeatPath, rawBeatPath)
                .let { HeatmapSwipeStrokeOrderPath_v2.filterToStrokeOrder(it, visitOrder) }
                .let { if (it.size >= visitOrder.size - 1) it else visitOrder }
            return HeatmapPathLettersNormalize_v2.Normalized(
                letters = capTripleRuns(path),
                dwellHints = emptyList(),
            )
        }
        return HeatmapPathLettersNormalize_v8.normalize(
            raw = rawBeatPath,
            neighborGraph = neighborGraph,
            layout = layout,
            pointers = pointers,
            touch = HeatmapSwipeStrokeTouchSet_v3.Result(
                touched = touch.touched,
                counts = touch.counts,
                orderedLetters = touch.orderedLetters,
                rejectedTouchLetters = touch.rejectedTouchLetters,
                startLabel = touch.startLabel,
                liftLabel = touch.liftLabel,
            ),
        )
    }

    @JvmStatic
    fun isMultiCornerStroke(
        beatCountRaw: Int,
        beatPath: List<String>,
        visitOrder: List<String>,
    ): Boolean =
        beatCountRaw >= MULTI_CORNER_BEAT_MIN ||
            beatPath.size >= MULTI_CORNER_PATH_MIN ||
            visitOrder.size >= MULTI_CORNER_PATH_MIN + 1

    private fun pickLongestPath(vararg paths: List<String>): List<String> =
        paths.filter { it.isNotEmpty() }.maxByOrNull { it.size } ?: emptyList()

    private fun capTripleRuns(path: List<String>): List<String> {
        if (path.size < 3) return path
        val out = ArrayList<String>()
        var dupRun = 0
        for (letter in path) {
            if (out.isNotEmpty() && out.last() == letter) {
                dupRun++
                if (dupRun >= 2) continue
            } else {
                dupRun = 0
            }
            out.add(letter)
        }
        return out
    }
}
