// SPDX-License-Identifier: GPL-3.0-only

// ai-note: Block 3 step 15t — v11 + force path[0] to touch-down start when touched

package helium314.keyboard.heatmap.swipe

object HeatmapPathLettersNormalize_v12 {

    @JvmStatic
    fun normalize(
        cornerPath: List<String>,
        neighborGraph: HeatmapKeyNeighborGraph_v2.Graph?,
        touch: HeatmapSwipeStrokeTouchSet_v5.Result,
        startLabel: String?,
    ): HeatmapPathLettersNormalize_v2.Normalized {
        val base = HeatmapPathLettersNormalize_v11.normalize(cornerPath, neighborGraph, touch)
        val anchored = HeatmapSwipeStartLetterAnchor_v1.anchorPathLetters(
            base.letters, startLabel, touch.touched,
        )
        return HeatmapPathLettersNormalize_v2.Normalized(
            letters = anchored,
            dwellHints = base.dwellHints,
        )
    }
}
