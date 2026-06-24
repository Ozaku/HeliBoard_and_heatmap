// SPDX-License-Identifier: GPL-3.0-only

// ai-note: Block 3 step 15b — ordered subsequence middle letters; v1 weights retained

package helium314.keyboard.heatmap.swipe

object HeatmapLiteralSwipeScore_v2 {

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
        var matchedWeight = 0.0
        var totalWeight = 0.0
        for (i in lower.indices) {
            val w = HeatmapLiteralSwipeScore_v1.letterWeight(i, len)
            totalWeight += w
            val ch = lower[i].toString()
            val (isMatch, nextIdx) = when (i) {
                0 -> {
                    val ok = startLabel == null || ch == startLabel
                    val ni = if (ok && pathLetters.firstOrNull() == ch) 1 else 0
                    ok to ni
                }
                else -> matchMiddle(ch, pathLetters, pathIdx)
            }
            if (isMatch) matchedWeight += w
            pathIdx = nextIdx
        }
        if (totalWeight <= 0.0) return 0.0
        return matchedWeight / totalWeight
    }

    private fun matchMiddle(
        ch: String,
        pathLetters: List<String>,
        pathIdx: Int,
    ): Pair<Boolean, Int> {
        val idx = findForward(pathLetters, pathIdx, ch)
        return if (idx >= 0) true to (idx + 1) else false to pathIdx
    }

    private fun findForward(path: List<String>, from: Int, letter: String): Int {
        for (j in from until path.size) {
            if (path[j] == letter) return j
        }
        return -1
    }
}
