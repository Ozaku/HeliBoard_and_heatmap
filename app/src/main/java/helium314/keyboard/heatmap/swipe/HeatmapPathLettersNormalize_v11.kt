// SPDX-License-Identifier: GPL-3.0-only

// ai-note: Block 3 step 15s — literal corner path only; NO wiggle doubles injected into path (v7/v10 bug)

package helium314.keyboard.heatmap.swipe

import helium314.keyboard.heatmap.learning.HeatmapCoordinateMap_v1
import helium314.keyboard.latin.common.InputPointers

object HeatmapPathLettersNormalize_v11 {

    @JvmStatic
    fun normalize(
        cornerPath: List<String>,
        neighborGraph: HeatmapKeyNeighborGraph_v2.Graph?,
        touch: HeatmapSwipeStrokeTouchSet_v5.Result,
    ): HeatmapPathLettersNormalize_v2.Normalized {
        var path = HeatmapSwipePathChainFilter_v1.filterSpuriousMiddleKeys(cornerPath, neighborGraph)
        path = HeatmapSwipeCornerPathBuilder_v1.mergeShortStrokeGaps(
            path, touch.orderedLetters, neighborGraph,
        )
        path = HeatmapPathLettersNormalize_v2.collapseBridgeMiddleKeys(path, neighborGraph)
        path = HeatmapSwipeStrokeOrderPath_v2.filterToStrokeOrder(path, touch.orderedLetters)
        return HeatmapPathLettersNormalize_v2.Normalized(
            letters = path,
            dwellHints = emptyList(),
        )
    }
}
