// SPDX-License-Identifier: GPL-3.0-only

// ai-note: Block 3 step 13d — letter position weights per 44_ (first strict, middle loose, last moderate)

package helium314.keyboard.heatmap.swipe

object HeatmapLiteralSwipeScore_v1 {

    fun letterWeight(index: Int, wordLen: Int): Double {
        if (wordLen <= 0) return 0.0
        return when (index) {
            0 -> 1.0
            wordLen - 1 -> 0.55
            else -> 0.25 / (1.0 + 0.15 * (wordLen - 3).coerceAtLeast(0))
        }
    }

    /**
     * Score candidate word against inferred path letters.
     * Start letter must match [startLabel] when provided (hard gate).
     */
    fun scoreWord(
        candidate: String,
        pathLetters: List<String>,
        startLabel: String?,
        endLabel: String?,
    ): Double {
        if (candidate.isEmpty()) return Double.NEGATIVE_INFINITY
        val lower = candidate.lowercase()
        if (!startLabel.isNullOrEmpty() && lower.firstOrNull()?.toString() != startLabel) {
            return Double.NEGATIVE_INFINITY
        }
        var total = 0.0
        var matched = 0.0
        val len = lower.length
        for (i in lower.indices) {
            val w = letterWeight(i, len)
            val ch = lower[i].toString()
            val ok = when (i) {
                0 -> startLabel == null || ch == startLabel
                len - 1 -> endLabel == null || ch == endLabel || pathLetters.contains(ch)
                else -> pathLetters.contains(ch)
            }
            if (ok) matched += w
            total += w
        }
        if (total <= 0.0) return 0.0
        return matched / total
    }
}
