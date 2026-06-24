// SPDX-License-Identifier: GPL-3.0-only

// ai-note: Block 3 step 15t — v1 + first letter must equal touch-down start anchor

package helium314.keyboard.heatmap.swipe

object HeatmapSwipeWordTouchGate_v2 {

    @JvmStatic
    fun isAllowed(
        candidate: String,
        touchedLetters: Set<String>,
        startLabel: String?,
        dwellDoubleLetters: Set<Char> = emptySet(),
    ): Boolean {
        if (!HeatmapSwipeWordTouchGate_v1.isAllowed(candidate, touchedLetters, dwellDoubleLetters)) {
            return false
        }
        return HeatmapSwipeStartLetterAnchor_v1.wordStartsWithAnchor(candidate, startLabel)
    }
}
