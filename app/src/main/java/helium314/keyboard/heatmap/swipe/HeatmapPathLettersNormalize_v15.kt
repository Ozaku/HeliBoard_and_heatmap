// SPDX-License-Identifier: GPL-3.0-only

// ai-note: v15 — single ordered corner path for lookup; no visit-order row expansion

package helium314.keyboard.heatmap.swipe

object HeatmapPathLettersNormalize_v15 {

    data class Normalized(
        val letters: List<String>,
        val intentPathLetters: List<String>,
        val dwellHints: List<HeatmapPathLettersNormalize_v2.DwellHint>,
        val orderedCornerCount: Int,
    )

    @JvmStatic
    fun normalize(
        classifiedBeats: List<HeatmapGeometryClassifier_v2.ClassifiedBeat>,
        neighborGraph: HeatmapKeyNeighborGraph_v2.Graph?,
        touch: HeatmapSwipeStrokeTouchSet_v6.Result,
        kinematics: HeatmapSwipeStrokeKinematics_v1.Result,
        layout: helium314.keyboard.heatmap.learning.HeatmapCoordinateMap_v1.Snapshot,
        pointers: helium314.keyboard.latin.common.InputPointers,
    ): Normalized {
        var path = HeatmapSwipeOrderedCornerPath_v1.build(
            layout = layout,
            classifiedBeats = classifiedBeats,
            dwellSegments = kinematics.dwellSegments,
            startDistribution = touch.startDistribution,
            liftLabel = touch.liftLabel,
        )
        path = HeatmapSwipeStartLetterSoftAnchor_v1.anchorPath(
            path, touch.startDistribution, touch.touched,
        )
        path = HeatmapSwipePathChainFilter_v1.filterSpuriousMiddleKeys(path, neighborGraph)
        path = HeatmapSwipeStrokeMonotonicPath_v1.filterOrderedPath(path)
        val dwellHints = HeatmapSwipePointerDwell_v2.detect(
            layout, pointers, path,
            beatIndices = kinematics.dwellSegments.map { it.startIndex },
        )
        return Normalized(
            letters = path,
            intentPathLetters = path,
            dwellHints = dwellHints,
            orderedCornerCount = path.size,
        )
    }
}
