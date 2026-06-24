// SPDX-License-Identifier: GPL-3.0-only

// ai-note: v1 — reject short all-caps acronyms when corner path implies a real word

package helium314.keyboard.heatmap.swipe

object HeatmapSwipeAcronymGuard_v1 {

    @JvmStatic
    fun rejectShortAcronym(candidate: String, orderedCornerCount: Int): Boolean {
        if (orderedCornerCount < 4) return false
        val lettersOnly = HeatmapSwipeContractionExpand_v1.lettersOnly(candidate)
        if (lettersOnly.length > 3) return false
        return candidate.length == lettersOnly.length &&
            lettersOnly.all { it.isUpperCase() }
    }
}