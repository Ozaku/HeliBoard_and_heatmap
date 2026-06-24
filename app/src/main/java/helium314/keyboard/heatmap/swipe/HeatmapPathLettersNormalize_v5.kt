// SPDX-License-Identifier: GPL-3.0-only

// ai-note: Block 3 step 15l — v2 touch counts + stroke-order path filter

package helium314.keyboard.heatmap.swipe

import helium314.keyboard.heatmap.learning.HeatmapCoordinateMap_v1
import helium314.keyboard.latin.common.InputPointers

object HeatmapPathLettersNormalize_v5 {

    @JvmStatic
    fun normalize(
        raw: List<String>,
        neighborGraph: HeatmapKeyNeighborGraph_v2.Graph?,
        layout: HeatmapCoordinateMap_v1.Snapshot,
        pointers: InputPointers,
        touch: HeatmapSwipeStrokeTouchSet_v2.Result,
        beatIndices: List<Int> = emptyList(),
    ): HeatmapPathLettersNormalize_v2.Normalized {
        val chained = HeatmapSwipePathChainFilter_v1.filterSpuriousMiddleKeys(raw, neighborGraph)
        val touchFilteredRaw = HeatmapSwipeStrokeTouchSet_v2.filterPathToTouched(chained, touch.touched)
        val base = HeatmapPathLettersNormalize_v2.normalize(touchFilteredRaw, neighborGraph)
        var letters = HeatmapSwipeStrokeTouchSet_v2.filterPathToTouched(base.letters, touch.touched)
        letters = HeatmapSwipeStrokeOrderPath_v1.filterToStrokeOrder(letters, touch.orderedLetters)
        val pointerHints = HeatmapSwipePointerDwell_v2.detect(layout, pointers, letters, beatIndices)
        val merged = mergeDwellHints(base.dwellHints, pointerHints)
        return HeatmapPathLettersNormalize_v2.Normalized(
            letters = letters,
            dwellHints = merged.filter { it.letter in touch.touched },
        )
    }

    private fun mergeDwellHints(
        labelHints: List<HeatmapPathLettersNormalize_v2.DwellHint>,
        pointerHints: List<HeatmapPathLettersNormalize_v2.DwellHint>,
    ): List<HeatmapPathLettersNormalize_v2.DwellHint> {
        val byIndex = LinkedHashMap<Int, HeatmapPathLettersNormalize_v2.DwellHint>()
        for (hint in labelHints + pointerHints) {
            val prev = byIndex[hint.dedupedIndex]
            if (prev == null || hint.rawRunLength > prev.rawRunLength) {
                byIndex[hint.dedupedIndex] = hint
            }
        }
        return byIndex.values.toList()
    }
}
