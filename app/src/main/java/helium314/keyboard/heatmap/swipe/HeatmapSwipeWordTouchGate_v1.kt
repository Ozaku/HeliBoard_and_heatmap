// SPDX-License-Identifier: GPL-3.0-only

// ai-note: Block 3 step 15l — every output letter must be substantively touched; blocks fat when a not touched

package helium314.keyboard.heatmap.swipe

object HeatmapSwipeWordTouchGate_v1 {

    @JvmStatic
    fun isAllowed(
        candidate: String,
        touchedLetters: Set<String>,
        dwellDoubleLetters: Set<Char> = emptySet(),
    ): Boolean {
        if (candidate.isEmpty() || touchedLetters.isEmpty()) return false
        val lower = HeatmapSwipeContractionExpand_v1.lettersOnly(candidate)
        if (lower.isEmpty()) return false
        var i = 0
        while (i < lower.length) {
            val letter = lower[i]
            val letterStr = letter.toString()
            if (i > 0 && letter == lower[i - 1]) {
                if (letter in dwellDoubleLetters || letterStr in touchedLetters) {
                    while (i < lower.length && lower[i] == letter) i++
                    continue
                }
                return false
            }
            if (letterStr !in touchedLetters) return false
            i++
        }
        return true
    }
}
