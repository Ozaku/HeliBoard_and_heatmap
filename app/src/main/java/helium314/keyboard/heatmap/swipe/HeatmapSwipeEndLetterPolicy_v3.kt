// SPDX-License-Identifier: GPL-3.0-only

// ai-note: v3 — end anchor for long swipes + neighbor-tolerant lift match

package helium314.keyboard.heatmap.swipe

object HeatmapSwipeEndLetterPolicy_v3 {

    private const val SHORT_PATH_MAX = 4

    @JvmStatic
    fun requiresEndMatch(
        infer: HeatmapSwipeSegmentInfer_v19.Result,
    ): Boolean {
        val intent = infer.intentPathLetters
        val letters = if (intent.isNotEmpty()) intent else infer.pathLetters
        val strokeLen = infer.strokeOrderLetters.size
        val intentLen = intent.size
        val pathLen = infer.pathLetters.size
        val end = infer.endKeyLabel ?: return false
        if (end.isEmpty() || end !in infer.touchedLetters) return false

        if (strokeLen >= 6 && intentLen >= 3) return true
        if (strokeLen >= 5 && strokeLen > intentLen + 1) return true
        if (pathLen > SHORT_PATH_MAX && strokeLen >= pathLen + 2) return true
        if (intentLen >= 4 && strokeLen >= intentLen) return true

        if (letters.size > SHORT_PATH_MAX || letters.size < 3) return false
        val hasAdjacentDouble = letters.zipWithNext().any { (a, b) -> a == b }
        val wiggleBeats = infer.beatCountRaw > letters.size + 1
        return hasAdjacentDouble || wiggleBeats
    }

    @JvmStatic
    fun requiresEndMatch(inferV12: HeatmapSwipeSegmentInfer_v12.Result): Boolean =
        HeatmapSwipeEndLetterPolicy_v2.requiresEndMatch(inferV12)

    @JvmStatic
    fun wordEndsOnLift(
        candidate: String,
        endLabel: String?,
        neighborGraph: HeatmapKeyNeighborGraph_v2.Graph? = null,
    ): Boolean {
        if (endLabel.isNullOrEmpty()) return true
        val lower = HeatmapSwipeContractionExpand_v1.lettersOnly(candidate)
        if (lower.isEmpty()) return false
        val last = lower.lastOrNull()?.toString() ?: return false
        if (last == endLabel) return true
        return neighborGraph != null &&
            HeatmapKeyNeighborGraph_v2.areNeighbors(neighborGraph, last, endLabel)
    }
}
