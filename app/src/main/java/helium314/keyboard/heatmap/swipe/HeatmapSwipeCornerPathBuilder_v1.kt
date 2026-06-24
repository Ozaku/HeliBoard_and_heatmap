// SPDX-License-Identifier: GPL-3.0-only

// ai-note: Block 3 step 15r — path = corner beats only (v5 labels); NOT full pointer visit sweep

package helium314.keyboard.heatmap.swipe

import helium314.keyboard.heatmap.learning.HeatmapCoordinateMap_v1

object HeatmapSwipeCornerPathBuilder_v1 {

    @JvmStatic
    fun fromClassifiedBeats(
        layout: HeatmapCoordinateMap_v1.Snapshot,
        classified: List<HeatmapGeometryClassifier_v2.ClassifiedBeat>,
    ): List<String> {
        val labels = classified.mapNotNull { beat ->
            beat.keyLabel ?: HeatmapKeyLikelihood_v5.bestLabelAt(layout, beat.x, beat.y)
        }
        return HeatmapPathLettersNormalize_v1.collapseConsecutiveDuplicates(labels)
    }

    /** ai-note: only fill from stroke order when short neighbor chain and corner path is subsequence */
    @JvmStatic
    fun mergeShortStrokeGaps(
        cornerPath: List<String>,
        strokeOrder: List<String>,
        neighborGraph: HeatmapKeyNeighborGraph_v2.Graph?,
    ): List<String> {
        if (cornerPath.isEmpty() || strokeOrder.isEmpty()) return cornerPath
        if (strokeOrder.size > cornerPath.size + 2) return cornerPath
        if (!isNeighborChain(strokeOrder, neighborGraph)) return cornerPath
        if (!isSubsequence(cornerPath, strokeOrder)) return cornerPath
        return if (strokeOrder.size > cornerPath.size) strokeOrder else cornerPath
    }

    private fun isSubsequence(corner: List<String>, stroke: List<String>): Boolean {
        var ci = 0
        for (letter in stroke) {
            if (ci < corner.size && letter == corner[ci]) ci++
        }
        return ci == corner.size
    }

    private fun isNeighborChain(
        stroke: List<String>,
        neighborGraph: HeatmapKeyNeighborGraph_v2.Graph?,
    ): Boolean {
        if (stroke.size < 2) return true
        for (i in 1 until stroke.size) {
            if (!HeatmapKeyNeighborGraph_v2.areNeighbors(neighborGraph, stroke[i - 1], stroke[i])) {
                return false
            }
        }
        return true
    }
}
