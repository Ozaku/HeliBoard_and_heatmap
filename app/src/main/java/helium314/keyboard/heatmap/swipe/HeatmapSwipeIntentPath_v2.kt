// SPDX-License-Identifier: GPL-3.0-only

// ai-note: v2 — expand corner-only intent paths for long fast/moderate swipes via visit order

package helium314.keyboard.heatmap.swipe

object HeatmapSwipeIntentPath_v2 {

    /** ai-note: visit order shorter than this — keep corner-only path (4-letter swipes unchanged) */
    private const val LONG_VISIT_MIN = 5

    /** ai-note: collapsed intent at or below this with long visit triggers expansion */
    private const val SHORT_INTENT_MAX = 4

    /** ai-note: min gap between visit keys and intent keys before we trust visit order */
    private const val MIN_VISIT_INTENT_GAP = 2

    @JvmStatic
    fun build(
        intent: HeatmapSwipeIntentClassifier_v2.Result,
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
        val visit = touch.orderedLetters
        when {
            kinematics.isSlowStroke && path.size < visit.size - 1 -> {
                path = visitOrderFallback(path, visit)
            }
            needsLongStrokeVisitExpand(path, visit, kinematics) -> {
                path = visitOrderFallback(path, visit)
            }
        }
        path = HeatmapSwipeStrokeOrderPath_v2.filterToStrokeOrder(path, visit)
        return path
    }

    /** ai-note: fast swipe across 6+ keys often yields ~4 corner letters; visit order has full trace */
    private fun needsLongStrokeVisitExpand(
        path: List<String>,
        visit: List<String>,
        kinematics: HeatmapSwipeStrokeKinematics_v1.Result,
    ): Boolean {
        if (kinematics.isSlowStroke) return false
        if (visit.size < LONG_VISIT_MIN) return false
        if (path.size >= visit.size - 1) return false
        if (path.size <= SHORT_INTENT_MAX && visit.size - path.size >= MIN_VISIT_INTENT_GAP) {
            return true
        }
        return visit.size - path.size >= 3
    }

    private fun visitOrderFallback(
        intentPath: List<String>,
        visitOrder: List<String>,
    ): List<String> {
        if (visitOrder.isEmpty()) return intentPath
        if (intentPath.isEmpty()) return visitOrder
        val aligned = HeatmapSwipeStrokeOrderPath_v2.filterToStrokeOrder(intentPath, visitOrder)
        return if (aligned.size >= visitOrder.size - 1) aligned else visitOrder
    }
}

