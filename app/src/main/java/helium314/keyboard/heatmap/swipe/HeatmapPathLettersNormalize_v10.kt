// SPDX-License-Identifier: GPL-3.0-only

// ai-note: Block 3 step 15r — corner path + chain/bridge/wiggle; NO visit-order-as-path (v9 disaster)

package helium314.keyboard.heatmap.swipe

import helium314.keyboard.heatmap.learning.HeatmapCoordinateMap_v1
import helium314.keyboard.latin.common.InputPointers

object HeatmapPathLettersNormalize_v10 {

    @JvmStatic
    fun normalize(
        cornerPath: List<String>,
        neighborGraph: HeatmapKeyNeighborGraph_v2.Graph?,
        layout: HeatmapCoordinateMap_v1.Snapshot,
        pointers: InputPointers,
        touch: HeatmapSwipeStrokeTouchSet_v5.Result,
    ): HeatmapPathLettersNormalize_v2.Normalized {
        var path = HeatmapSwipePathChainFilter_v1.filterSpuriousMiddleKeys(cornerPath, neighborGraph)
        path = HeatmapSwipeCornerPathBuilder_v1.mergeShortStrokeGaps(
            path, touch.orderedLetters, neighborGraph,
        )
        path = HeatmapPathLettersNormalize_v2.collapseBridgeMiddleKeys(path, neighborGraph)
        path = preserveSameKeyWiggleRuns(cornerPath, path)
        val wiggleHints = HeatmapSwipeKeyWiggleDetector_v1.detect(layout, pointers, path)
        path = HeatmapSwipeKeyWiggleDetector_v1.expandPathDoubles(path, wiggleHints)
        path = HeatmapSwipeStrokeOrderPath_v2.filterToStrokeOrder(path, touch.orderedLetters)
        return HeatmapPathLettersNormalize_v2.Normalized(
            letters = path,
            dwellHints = emptyList(),
        )
    }

    private fun preserveSameKeyWiggleRuns(
        raw: List<String>,
        bridged: List<String>,
    ): List<String> {
        if (raw.size < 2) return bridged
        val expanded = ArrayList<String>()
        var i = 0
        while (i < raw.size) {
            val letter = raw[i]
            var run = 1
            while (i + run < raw.size && raw[i + run] == letter) run++
            expanded.add(letter)
            if (run >= 2) expanded.add(letter)
            i += run
        }
        return capTripleRuns(expanded.ifEmpty { bridged })
    }

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
