// SPDX-License-Identifier: GPL-3.0-only

// ai-note: Block 3 step 15i — reachable pool gate before align; feet allowed, fat rejected on f>e>t

package helium314.keyboard.heatmap.swipe

object HeatmapLiteralSwipeScore_v7 {

    private const val EXACT_WEIGHT = 1.0
    private const val NEIGHBOR_WEIGHT = 0.58
    private const val INSERT_WEIGHT = 0.7
    private const val DOUBLE_LETTER_BOOST = 1.08
    private const val SKIP_PATH_WEIGHT = 0.48
    private const val MAX_INSERTS = 2
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
        if (!HeatmapSwipeReachableLetters_v1.isCandidateReachable(
                candidate, pathLetters, neighborGraph, dwellDoubleLetters,
            )
        ) {
            return Double.NEGATIVE_INFINITY
        }
        val base = alignScore(lower, pathLetters, neighborGraph, dwellDoubleLetters)
        if (base <= 0.0) return base
        if (hasIntentionalDouble(lower, dwellDoubleLetters)) return base * DOUBLE_LETTER_BOOST
        return base
    }

    private fun hasIntentionalDouble(lower: String, dwellDoubleLetters: Set<Char>): Boolean {
        for (i in 1 until lower.length) {
            if (lower[i] == lower[i - 1] && lower[i] in dwellDoubleLetters) return true
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
        var insertsUsed = 0
        var skipsUsed = 0
        for (i in candidate.indices) {
            val w = HeatmapLiteralSwipeScore_v1.letterWeight(i, len)
            total += w
            val ch = candidate[i].toString()
            if (i > 0 && candidate[i] == candidate[i - 1] && candidate[i] in dwellDoubleLetters) {
                matched += w * EXACT_WEIGHT
                continue
            }
            val fit = consumeLetter(ch, path, pathIdx, neighborGraph, insertsUsed, skipsUsed)
            if (fit != null) {
                matched += w * fit.weight
                pathIdx = fit.nextPathIdx
                insertsUsed = fit.insertsUsed
                skipsUsed = fit.skipsUsed
            }
        }
        if (total <= 0.0) return 0.0
        return matched / total
    }

    private data class Fit(
        val weight: Double,
        val nextPathIdx: Int,
        val insertsUsed: Int,
        val skipsUsed: Int,
    )

    private fun consumeLetter(
        ch: String,
        path: List<String>,
        pathIdx: Int,
        neighborGraph: HeatmapKeyNeighborGraph_v2.Graph?,
        insertsUsed: Int,
        skipsUsed: Int,
    ): Fit? {
        if (pathIdx >= path.size) {
            return if (insertsUsed < MAX_INSERTS) {
                Fit(INSERT_WEIGHT, pathIdx, insertsUsed + 1, skipsUsed)
            } else {
                null
            }
        }
        val options = ArrayList<Fit>(4)
        val exact = findFrom(path, pathIdx, ch) { it == ch }
        if (exact >= 0) options.add(Fit(EXACT_WEIGHT, exact + 1, insertsUsed, skipsUsed))
        val neighbor = findFrom(path, pathIdx, ch) {
            HeatmapKeyNeighborGraph_v2.areNeighbors(neighborGraph, it, ch)
        }
        if (neighbor >= 0) options.add(Fit(NEIGHBOR_WEIGHT, neighbor + 1, insertsUsed, skipsUsed))
        if (insertsUsed < MAX_INSERTS) {
            options.add(Fit(INSERT_WEIGHT, pathIdx, insertsUsed + 1, skipsUsed))
        }
        if (skipsUsed < MAX_SKIPS && pathIdx + 1 < path.size) {
            val afterSkip = consumeLetter(ch, path, pathIdx + 1, neighborGraph, insertsUsed, skipsUsed + 1)
            if (afterSkip != null) {
                options.add(
                    Fit(
                        SKIP_PATH_WEIGHT * afterSkip.weight,
                        afterSkip.nextPathIdx,
                        afterSkip.insertsUsed,
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
