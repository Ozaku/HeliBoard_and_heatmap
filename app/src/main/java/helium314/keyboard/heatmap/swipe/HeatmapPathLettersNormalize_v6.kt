// SPDX-License-Identifier: GPL-3.0-only

// ai-note: Block 3 step 15m — no pointer/label dwell hints; chain filter + touch filter only

package helium314.keyboard.heatmap.swipe

import helium314.keyboard.heatmap.learning.HeatmapCoordinateMap_v1
import helium314.keyboard.latin.common.InputPointers

object HeatmapPathLettersNormalize_v6 {

    @JvmStatic
    fun normalize(
        raw: List<String>,
        neighborGraph: HeatmapKeyNeighborGraph_v2.Graph?,
        layout: HeatmapCoordinateMap_v1.Snapshot,
        pointers: InputPointers,
        touch: HeatmapSwipeStrokeTouchSet_v2.Result,
    ): HeatmapPathLettersNormalize_v2.Normalized {
        val chained = HeatmapSwipePathChainFilter_v1.filterSpuriousMiddleKeys(raw, neighborGraph)
        val touchFilteredRaw = HeatmapSwipeStrokeTouchSet_v2.filterPathToTouched(chained, touch.touched)
        val base = HeatmapPathLettersNormalize_v2.normalize(touchFilteredRaw, neighborGraph)
        var letters = HeatmapSwipeStrokeTouchSet_v2.filterPathToTouched(base.letters, touch.touched)
        letters = HeatmapSwipeStrokeOrderPath_v1.filterToStrokeOrder(letters, touch.orderedLetters)
        return HeatmapPathLettersNormalize_v2.Normalized(
            letters = letters,
            dwellHints = emptyList(),
        )
    }
}
