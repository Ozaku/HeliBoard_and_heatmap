// SPDX-License-Identifier: GPL-3.0-only

// ai-note: Block 3 step 15k — normalize then strip any letter not in stroke touch set (25% rule)

package helium314.keyboard.heatmap.swipe

import helium314.keyboard.heatmap.learning.HeatmapCoordinateMap_v1
import helium314.keyboard.latin.common.InputPointers

object HeatmapPathLettersNormalize_v4 {

    @JvmStatic
    fun normalize(
        raw: List<String>,
        neighborGraph: HeatmapKeyNeighborGraph_v2.Graph?,
        layout: HeatmapCoordinateMap_v1.Snapshot,
        pointers: InputPointers,
        beatIndices: List<Int> = emptyList(),
    ): HeatmapPathLettersNormalize_v2.Normalized {
        val touched = HeatmapSwipeStrokeTouchSet_v1.collect(layout, pointers)
        val chained = HeatmapSwipePathChainFilter_v1.filterSpuriousMiddleKeys(raw, neighborGraph)
        val touchFilteredRaw = HeatmapSwipeStrokeTouchSet_v1.filterPathToTouched(chained, touched)
        val base = HeatmapPathLettersNormalize_v2.normalize(touchFilteredRaw, neighborGraph)
        val touchFilteredLetters = HeatmapSwipeStrokeTouchSet_v1.filterPathToTouched(base.letters, touched)
        val pointerHints = HeatmapSwipePointerDwell_v2.detect(layout, pointers, touchFilteredLetters, beatIndices)
        val merged = mergeDwellHints(base.dwellHints, pointerHints)
        return HeatmapPathLettersNormalize_v2.Normalized(
            letters = touchFilteredLetters,
            dwellHints = merged.filter { it.letter in touched },
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
