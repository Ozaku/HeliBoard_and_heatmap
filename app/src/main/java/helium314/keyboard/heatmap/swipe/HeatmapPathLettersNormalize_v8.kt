// SPDX-License-Identifier: GPL-3.0-only

// ai-note: Block 3 step 15p — stroke-order backbone so tail keys (g, n) are not stripped from path

package helium314.keyboard.heatmap.swipe

import helium314.keyboard.heatmap.learning.HeatmapCoordinateMap_v1
import helium314.keyboard.latin.common.InputPointers

object HeatmapPathLettersNormalize_v8 {

    @JvmStatic
    fun normalize(
        raw: List<String>,
        neighborGraph: HeatmapKeyNeighborGraph_v2.Graph?,
        layout: HeatmapCoordinateMap_v1.Snapshot,
        pointers: InputPointers,
        touch: HeatmapSwipeStrokeTouchSet_v3.Result,
    ): HeatmapPathLettersNormalize_v2.Normalized {
        val chained = HeatmapSwipePathChainFilter_v1.filterSpuriousMiddleKeys(raw, neighborGraph)
        val strokeAnchored = anchorPathToStrokeOrder(chained, touch.orderedLetters)
        var bridged = HeatmapPathLettersNormalize_v2.collapseBridgeMiddleKeys(
            strokeAnchored, neighborGraph,
        )
        bridged = preserveSameKeyWiggleRuns(strokeAnchored, bridged)
        val wiggleHints = HeatmapSwipeKeyWiggleDetector_v1.detect(layout, pointers, bridged)
        bridged = HeatmapSwipeKeyWiggleDetector_v1.expandPathDoubles(bridged, wiggleHints)
        bridged = HeatmapSwipeStrokeOrderPath_v2.filterToStrokeOrder(bridged, touch.orderedLetters)
        if (bridged.size < touch.orderedLetters.size) {
            bridged = mergeDoublesIntoStrokeOrder(touch.orderedLetters, bridged)
        }
        return HeatmapPathLettersNormalize_v2.Normalized(
            letters = bridged,
            dwellHints = emptyList(),
        )
    }

    /** ai-note: beat path may miss tail keys after touch filter — stroke order is the visit backbone */
    private fun anchorPathToStrokeOrder(
        beatPath: List<String>,
        strokeOrder: List<String>,
    ): List<String> {
        if (strokeOrder.isEmpty()) return beatPath
        if (beatPath.isEmpty()) return strokeOrder
        val aligned = HeatmapSwipeStrokeOrderPath_v2.filterToStrokeOrder(beatPath, strokeOrder)
        return if (aligned.size >= strokeOrder.size - 1) aligned else strokeOrder
    }

    private fun mergeDoublesIntoStrokeOrder(
        strokeOrder: List<String>,
        beatPath: List<String>,
    ): List<String> {
        if (strokeOrder.isEmpty()) return beatPath
        val out = ArrayList<String>(strokeOrder.size + 2)
        var beatIdx = 0
        for (key in strokeOrder) {
            out.add(key)
            while (beatIdx < beatPath.size && beatPath[beatIdx] != key) beatIdx++
            if (beatIdx < beatPath.size && beatPath[beatIdx] == key) {
                if (beatIdx + 1 < beatPath.size && beatPath[beatIdx + 1] == key) {
                    out.add(key)
                    beatIdx++
                }
                beatIdx++
            }
        }
        return capTripleRuns(out)
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
