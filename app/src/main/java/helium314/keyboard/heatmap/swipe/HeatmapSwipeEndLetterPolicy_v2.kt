// SPDX-License-Identifier: GPL-3.0-only

// ai-note: Block 3 step 15p — strict lift-end only for short wiggle paths (feet/fee); not feeding/typing

package helium314.keyboard.heatmap.swipe

object HeatmapSwipeEndLetterPolicy_v2 {

    private const val SHORT_PATH_MAX = 4

    @JvmStatic
    fun requiresEndMatch(infer: HeatmapSwipeSegmentInfer_v12.Result): Boolean {
        val letters = infer.pathLetters
        if (letters.size > SHORT_PATH_MAX || letters.size < 3) return false
        val end = infer.endKeyLabel ?: return false
        if (end.isEmpty() || end !in infer.touchedLetters) return false
        val hasAdjacentDouble = letters.zipWithNext().any { (a, b) -> a == b }
        val wiggleBeats = infer.beatCountRaw > letters.size + 1
        return hasAdjacentDouble || wiggleBeats
    }

    @JvmStatic
    fun wordEndsOnLift(candidate: String, endLabel: String?): Boolean {
        if (endLabel.isNullOrEmpty()) return true
        val lower = HeatmapSwipeContractionExpand_v1.lettersOnly(candidate)
        if (lower.isEmpty()) return false
        return lower.lastOrNull()?.toString() == endLabel
    }
}
