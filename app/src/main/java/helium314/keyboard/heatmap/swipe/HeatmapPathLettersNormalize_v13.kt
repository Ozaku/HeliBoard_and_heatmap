// SPDX-License-Identifier: GPL-3.0-only

// ai-note: Phase1 step 3.3 — intent path normalize with soft start; replaces v12 corner-only path

package helium314.keyboard.heatmap.swipe

object HeatmapPathLettersNormalize_v13 {

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
    ): Normalized {
        var path = HeatmapSwipeIntentPath_v1.build(
            intent = HeatmapSwipeIntentClassifier_v1.Result(
                segments = emptyList(),
                visitOrder = touch.orderedLetters,
                transitKeys = emptySet(),
                intentLetters = intentPath,
                startDistribution = touch.startDistribution,
                liftLabel = touch.liftLabel,
            ),
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
            intentPathLetters = path,
            dwellHints = dwellHints,
        )
    }
}
