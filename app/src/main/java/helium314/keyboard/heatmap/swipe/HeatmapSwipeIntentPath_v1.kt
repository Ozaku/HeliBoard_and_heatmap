// SPDX-License-Identifier: GPL-3.0-only

// ai-note: Phase1 step 3.1 — intent path from dwell/corners; slow-stroke visit fallback; fast dict gaps

package helium314.keyboard.heatmap.swipe

object HeatmapSwipeIntentPath_v1 {

    @JvmStatic
    fun build(
        intent: HeatmapSwipeIntentClassifier_v1.Result,
        touch: HeatmapSwipeStrokeTouchSet_v6.Result,
        cornerPath: List<String>,
        kinematics: HeatmapSwipeStrokeKinematics_v1.Result,
        neighborGraph: HeatmapKeyNeighborGraph_v2.Graph?,
    ): List<String> {
        var path: List<String> = if (intent.intentLetters.isNotEmpty()) {
            intent.intentLetters
        } else {
            cornerPath
        }
        path = HeatmapSwipeStartLetterSoftAnchor_v1.anchorPath(
            path, touch.startDistribution, touch.touched,
        )
        path = HeatmapSwipePathChainFilter_v1.filterSpuriousMiddleKeys(path, neighborGraph)
        if (kinematics.isSlowStroke && path.size < touch.orderedLetters.size - 1) {
            path = slowStrokeFallback(path, touch.orderedLetters)
        }
        path = HeatmapSwipeStrokeOrderPath_v2.filterToStrokeOrder(path, touch.orderedLetters)
        return path
    }

    private fun slowStrokeFallback(
        intentPath: List<String>,
        visitOrder: List<String>,
    ): List<String> {
        if (visitOrder.isEmpty()) return intentPath
        if (intentPath.isEmpty()) return visitOrder
        val aligned = HeatmapSwipeStrokeOrderPath_v2.filterToStrokeOrder(intentPath, visitOrder)
        return if (aligned.size >= visitOrder.size - 1) aligned else visitOrder
    }
}
