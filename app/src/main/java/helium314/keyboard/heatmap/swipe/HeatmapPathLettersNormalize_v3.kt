// SPDX-License-Identifier: GPL-3.0-only

// ai-note: Block 3 step 15j — chain filter + v2 normalize + pointer dwell merge

package helium314.keyboard.heatmap.swipe

import helium314.keyboard.heatmap.learning.HeatmapCoordinateMap_v1
import helium314.keyboard.latin.common.InputPointers

object HeatmapPathLettersNormalize_v3 {

    @JvmStatic
    fun normalize(
        raw: List<String>,
        neighborGraph: HeatmapKeyNeighborGraph_v2.Graph?,
        layout: HeatmapCoordinateMap_v1.Snapshot? = null,
        pointers: InputPointers? = null,
        beatIndices: List<Int> = emptyList(),
    ): HeatmapPathLettersNormalize_v2.Normalized {
        val chained = HeatmapSwipePathChainFilter_v1.filterSpuriousMiddleKeys(raw, neighborGraph)
        val base = HeatmapPathLettersNormalize_v2.normalize(chained, neighborGraph)
        val pointerHints = if (layout != null && pointers != null) {
            HeatmapSwipePointerDwell_v1.detect(layout, pointers, base.letters, beatIndices)
        } else {
            emptyList()
        }
        val merged = mergeDwellHints(base.dwellHints, pointerHints)
        return HeatmapPathLettersNormalize_v2.Normalized(
            letters = base.letters,
            dwellHints = merged,
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
