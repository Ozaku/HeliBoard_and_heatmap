// SPDX-License-Identifier: GPL-3.0-only

// ai-note: Block 3 step 15t — v5 + candidate first letter must match anchored path start

package helium314.keyboard.heatmap.swipe

object HeatmapSwipeReachableLetters_v6 {

    @JvmStatic
    fun isCandidateReachable(
        candidate: String,
        pathLetters: List<String>,
        requirePathEnd: Boolean,
        startLabel: String? = null,
    ): Boolean {
        if (!HeatmapSwipeReachableLetters_v5.isCandidateReachable(
                candidate, pathLetters, requirePathEnd,
            )
        ) {
            return false
        }
        val lower = HeatmapSwipeContractionExpand_v1.lettersOnly(candidate)
        if (lower.isEmpty() || pathLetters.isEmpty()) return false
        if (!startLabel.isNullOrEmpty()) {
            if (lower.first().toString() != startLabel.lowercase()) return false
            if (pathLetters.first().lowercase() != startLabel.lowercase()) return false
        }
        return true
    }
}
