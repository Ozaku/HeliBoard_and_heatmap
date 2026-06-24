// SPDX-License-Identifier: GPL-3.0-only

// ai-note: Block 3 step 15c — ordered align + same-path-index double letters + inversion penalty

package helium314.keyboard.heatmap.swipe

object HeatmapLiteralSwipeScore_v3 {

    private const val INVERSION_PENALTY = 0.22

    fun scoreWord(
        candidate: String,
        pathLetters: List<String>,
        startLabel: String?,
        endLabel: String?,
    ): Double {
        if (candidate.isEmpty() || pathLetters.isEmpty()) return Double.NEGATIVE_INFINITY
        val lower = candidate.lowercase()
        if (!startLabel.isNullOrEmpty() && lower.firstOrNull()?.toString() != startLabel) {
            return Double.NEGATIVE_INFINITY
        }
        val len = lower.length
        var pathIdx = 0
        var lastPathIdx = -1
        var inversions = 0
        var matchedWeight = 0.0
        var totalWeight = 0.0
        for (i in lower.indices) {
            val w = HeatmapLiteralSwipeScore_v1.letterWeight(i, len)
            totalWeight += w
            val ch = lower[i].toString()
            val (idx, matched) = when (i) {
                0 -> {
                    val ok = startLabel == null || ch == startLabel
                    if (!ok) -1 to false
                    else if (pathLetters.firstOrNull() == ch) 0 to true
                    else 0 to true
                }
                else -> resolveMatch(lower, i, ch, pathLetters, pathIdx)
            }
            if (matched) {
                matchedWeight += w
                if (idx >= 0) {
                    if (lastPathIdx >= 0 && idx < lastPathIdx) inversions++
                    if (idx > lastPathIdx) lastPathIdx = idx
                    pathIdx = if (isDoubleLetterRepeat(lower, i, pathLetters, idx)) idx else idx + 1
                }
            } else if (lastPathIdx >= 0 && i > 0 && lower[i] != lower[i - 1]) {
                val earliest = pathLetters.indexOf(ch)
                if (earliest >= 0 && earliest < lastPathIdx) inversions++
            }
        }
        if (totalWeight <= 0.0) return 0.0
        val base = matchedWeight / totalWeight
        val penalty = (1.0 - INVERSION_PENALTY * inversions).coerceAtLeast(0.35)
        return base * penalty
    }

    private fun resolveMatch(
        lower: String,
        i: Int,
        ch: String,
        pathLetters: List<String>,
        pathIdx: Int,
    ): Pair<Int, Boolean> {
        if (isDoubleLetterRepeat(lower, i, pathLetters, pathIdx)) {
            val prevIdx = (pathIdx - 1).coerceAtLeast(0)
            if (pathLetters.getOrNull(prevIdx) == ch) return prevIdx to true
        }
        val idx = findForward(pathLetters, pathIdx, ch)
        return if (idx >= 0) idx to true else -1 to false
    }

    private fun isDoubleLetterRepeat(
        lower: String,
        i: Int,
        pathLetters: List<String>,
        pathIdx: Int,
    ): Boolean {
        if (i <= 0 || lower[i] != lower[i - 1]) return false
        val prevIdx = (pathIdx - 1).coerceAtLeast(0)
        return pathLetters.getOrNull(prevIdx) == lower[i].toString()
    }

    private fun findForward(path: List<String>, from: Int, letter: String): Int {
        for (j in from until path.size) {
            if (path[j] == letter) return j
        }
        return -1
    }
}
