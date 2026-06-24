// SPDX-License-Identifier: GPL-3.0-only

// ai-note: Block 3 step 15n — lift-end letter is mandatory when detected (fee blocked when ending on T)

package helium314.keyboard.heatmap.swipe

object HeatmapSwipeEndLetterPolicy_v1 {

    @JvmStatic
    fun requiresEndMatch(infer: HeatmapSwipeSegmentInfer_v12.Result): Boolean {
        val end = infer.endKeyLabel ?: return false
        return end.isNotEmpty() && end in infer.touchedLetters
    }

    @JvmStatic
    fun wordEndsOnLift(candidate: String, endLabel: String?): Boolean {
        if (endLabel.isNullOrEmpty()) return true
        val lower = HeatmapSwipeContractionExpand_v1.lettersOnly(candidate)
        if (lower.isEmpty()) return false
        return lower.lastOrNull()?.toString() == endLabel
    }
}
