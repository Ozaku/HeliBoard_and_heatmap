// SPDX-License-Identifier: GPL-3.0-only

// ai-note: Block 3 step 15j — no off-path inserts; reachable v2 gate; stronger dwell double boost

package helium314.keyboard.heatmap.swipe

object HeatmapLiteralSwipeScore_v8 {

    private const val EXACT_WEIGHT = 1.0
    private const val NEIGHBOR_WEIGHT = 0.55
    private const val DOUBLE_LETTER_BOOST = 1.14
    private const val SKIP_PATH_WEIGHT = 0.46
    private const val MAX_SKIPS = 2

    fun scoreWord(
        candidate: String,
        pathLetters: List<String>,
        startLabel: String?,
        endLabel: String?,
        neighborGraph: HeatmapKeyNeighborGraph_v2.Graph? = null,
        dwellDoubleLetters: Set<Char> = emptySet(),
        strictEndMatch: Boolean = false,
    ): Double {
        if (candidate.isEmpty() || pathLetters.isEmpty()) return Double.NEGATIVE_INFINITY
        val lower = HeatmapSwipeContractionExpand_v1.lettersOnly(candidate)
        if (lower.isEmpty()) return Double.NEGATIVE_INFINITY
        if (!startLabel.isNullOrEmpty() && lower.firstOrNull()?.toString() != startLabel) {
            return Double.NEGATIVE_INFINITY
        }
        if (strictEndMatch && !endLabel.isNullOrEmpty() && lower.length >= 2) {
            if (lower.lastOrNull()?.toString() != endLabel) {
                return Double.NEGATIVE_INFINITY
            }
        }
        if (!endLabel.isNullOrEmpty() && lower.length >= 2) {
            if (lower.lastOrNull()?.toString() != endLabel) {
                return Double.NEGATIVE_INFINITY
            }
        }
        if (!HeatmapSwipeReachableLetters_v2.isCandidateReachable(
                candidate, pathLetters, neighborGraph, dwellDoubleLetters,
            )
        ) {
            return Double.NEGATIVE_INFINITY
        }
        val base = alignScore(lower, pathLetters, neighborGraph, dwellDoubleLetters)
        if (base <= 0.0) return base
        if (hasIntentionalDouble(lower, dwellDoubleLetters, pathLetters)) {
            return base * DOUBLE_LETTER_BOOST
        }
        return base
    }

    private fun hasIntentionalDouble(
        lower: String,
        dwellDoubleLetters: Set<Char>,
        pathLetters: List<String>,
    ): Boolean {
        val pathChars = pathLetters.mapNotNull { it.firstOrNull() }.toSet()
        for (i in 1 until lower.length) {
            if (lower[i] == lower[i - 1] && (lower[i] in dwellDoubleLetters || lower[i] in pathChars)) {
                return true
            }
        }
        return false
    }

    private fun alignScore(
        candidate: String,
        path: List<String>,
        neighborGraph: HeatmapKeyNeighborGraph_v2.Graph?,
        dwellDoubleLetters: Set<Char>,
    ): Double {
        val len = candidate.length
        var total = 0.0
        var matched = 0.0
        var pathIdx = 0
        var skipsUsed = 0
        for (i in candidate.indices) {
            val w = HeatmapLiteralSwipeScore_v1.letterWeight(i, len)
            total += w
            if (i > 0 && candidate[i] == candidate[i - 1]) {
                val letter = candidate[i]
                if (letter in dwellDoubleLetters || path.any { it.firstOrNull() == letter }) {
                    matched += w * EXACT_WEIGHT
                    continue
                }
            }
            val ch = candidate[i].toString()
            val fit = consumeLetter(ch, path, pathIdx, neighborGraph, skipsUsed)
            if (fit != null) {
                matched += w * fit.weight
                pathIdx = fit.nextPathIdx
                skipsUsed = fit.skipsUsed
            }
        }
        if (total <= 0.0) return 0.0
        return matched / total
    }

    private data class Fit(
        val weight: Double,
        val nextPathIdx: Int,
        val skipsUsed: Int,
    )

    private fun consumeLetter(
        ch: String,
        path: List<String>,
        pathIdx: Int,
        neighborGraph: HeatmapKeyNeighborGraph_v2.Graph?,
        skipsUsed: Int,
    ): Fit? {
        if (pathIdx >= path.size) return null
        val options = ArrayList<Fit>(3)
        val exact = findFrom(path, pathIdx, ch) { it == ch }
        if (exact >= 0) options.add(Fit(EXACT_WEIGHT, exact + 1, skipsUsed))
        val neighbor = findFrom(path, pathIdx, ch) {
            HeatmapKeyNeighborGraph_v2.areNeighbors(neighborGraph, it, ch)
        }
        if (neighbor >= 0) options.add(Fit(NEIGHBOR_WEIGHT, neighbor + 1, skipsUsed))
        if (skipsUsed < MAX_SKIPS && pathIdx + 1 < path.size) {
            val afterSkip = consumeLetter(ch, path, pathIdx + 1, neighborGraph, skipsUsed + 1)
            if (afterSkip != null) {
                options.add(
                    Fit(
                        SKIP_PATH_WEIGHT * afterSkip.weight,
                        afterSkip.nextPathIdx,
                        afterSkip.skipsUsed,
                    ),
                )
            }
        }
        return options.maxByOrNull { it.weight }
    }

    private fun findFrom(path: List<String>, from: Int, ch: String, pred: (String) -> Boolean): Int {
        for (j in from until path.size) {
            if (pred(path[j])) return j
        }
        return -1
    }
}
