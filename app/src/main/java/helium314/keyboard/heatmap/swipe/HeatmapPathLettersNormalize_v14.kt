// SPDX-License-Identifier: GPL-3.0-only

// ai-note: v14 — intent classifier v2 + intent path v2 for longer-word visit expansion

package helium314.keyboard.heatmap.swipe

object HeatmapPathLettersNormalize_v14 {

    data class Normalized(
        val letters: List<String>,
        val intentPathLetters: List<String>,
        val dwellHints: List<HeatmapPathLettersNormalize_v2.DwellHint>,
    )

    @JvmStatic
    fun normalize(
        intentPath: List<String>,
        neighborGraph: HeatmapKeyNeighborGraph_v2.Graph?,
        touch: HeatmapSwipeStrokeTouchSet_v6.Result,
        kinematics: HeatmapSwipeStrokeKinematics_v1.Result,
        layout: helium314.keyboard.heatmap.learning.HeatmapCoordinateMap_v1.Snapshot,
        pointers: helium314.keyboard.latin.common.InputPointers,
        intent: HeatmapSwipeIntentClassifier_v2.Result,
    ): Normalized {
        var path = HeatmapSwipeIntentPath_v2.build(
            intent = intent,
            touch = touch,
            cornerPath = intentPath,
            kinematics = kinematics,
            neighborGraph = neighborGraph,
        )
        path = HeatmapSwipeStartLetterSoftAnchor_v1.anchorPath(
            path, touch.startDistribution, touch.touched,
        )
        path = HeatmapPathLettersNormalize_v2.collapseBridgeMiddleKeys(path, neighborGraph)
        path = HeatmapSwipeStrokeOrderPath_v2.filterToStrokeOrder(path, touch.orderedLetters)
        val dwellHints = HeatmapSwipePointerDwell_v2.detect(
            layout, pointers, path,
            beatIndices = kinematics.dwellSegments.map { it.startIndex },
        )
        return Normalized(
            letters = path,
            intentPathLetters = intent.intentLetters,
            dwellHints = dwellHints,
        )
    }
}

