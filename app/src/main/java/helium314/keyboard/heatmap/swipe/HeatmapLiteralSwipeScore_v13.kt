// SPDX-License-Identifier: GPL-3.0-only

// ai-note: Block 3 step 15t — start anchor hard gate + early-letter keyboard hop limit from anchor

package helium314.keyboard.heatmap.swipe

object HeatmapLiteralSwipeScore_v13 {

    private const val EXACT_WEIGHT = 1.0
    private const val SKIP_PATH_WEIGHT = 0.46
    private const val MAX_SKIPS = 2
    private const val END_MATCH_BOOST = 1.1
    private const val START_MATCH_BOOST = 1.35

    fun scoreWord(
        candidate: String,
        pathLetters: List<String>,
        touchedLetters: Set<String>,
        startLabel: String?,
        endLabel: String?,
        requireEndMatch: Boolean,
        neighborGraph: HeatmapKeyNeighborGraph_v2.Graph? = null,
    ): Double {
        if (candidate.isEmpty() || pathLetters.isEmpty() || touchedLetters.isEmpty()) {
            return Double.NEGATIVE_INFINITY
        }
        if (!HeatmapSwipeWordTouchGate_v2.isAllowed(
                candidate, touchedLetters, startLabel, emptySet(),
            )
        ) {
            return Double.NEGATIVE_INFINITY
        }
        val lower = HeatmapSwipeContractionExpand_v1.lettersOnly(candidate)
        if (lower.isEmpty()) return Double.NEGATIVE_INFINITY
        if (!HeatmapSwipeStartLetterAnchor_v1.wordStartsWithAnchor(candidate, startLabel)) {
            return Double.NEGATIVE_INFINITY
        }
        for (i in 0 until minOf(2, lower.length)) {
            if (!HeatmapSwipeStartLetterAnchor_v1.isEarlyLetterReachable(
                    neighborGraph, startLabel, lower[i].toString(), i,
                )
            ) {
                return Double.NEGATIVE_INFINITY
            }
        }
        if (requireEndMatch && !endLabel.isNullOrEmpty() && lower.length >= 2) {
            if (lower.lastOrNull()?.toString() != endLabel) {
                return Double.NEGATIVE_INFINITY
            }
        }
        if (!HeatmapSwipeReachableLetters_v6.isCandidateReachable(
                candidate, pathLetters, requirePathEnd = requireEndMatch, startLabel = startLabel,
            )
        ) {
            return Double.NEGATIVE_INFINITY
        }
        var score = alignScore(lower, pathLetters)
        if (!startLabel.isNullOrEmpty() && lower.firstOrNull()?.toString() == startLabel) {
            score *= START_MATCH_BOOST
        }
        if (requireEndMatch && !endLabel.isNullOrEmpty() && lower.lastOrNull()?.toString() == endLabel) {
            score *= END_MATCH_BOOST
        }
        return score
    }

    private fun alignScore(candidate: String, path: List<String>): Double {
        val len = candidate.length
        var total = 0.0
        var matched = 0.0
        var pathIdx = 0
        var skipsUsed = 0
        for (i in candidate.indices) {
            val w = HeatmapLiteralSwipeScore_v1.letterWeight(i, len)
            total += w
            if (i > 0 && candidate[i] == candidate[i - 1]) {
                matched += w * EXACT_WEIGHT
                continue
            }
            val ch = candidate[i].toString()
            val fit = consumeLetter(ch, path, pathIdx, skipsUsed)
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
        skipsUsed: Int,
    ): Fit? {
        if (pathIdx >= path.size) return null
        val options = ArrayList<Fit>(2)
        val exact = findFrom(path, pathIdx, ch) { it == ch }
        if (exact >= 0) options.add(Fit(EXACT_WEIGHT, exact + 1, skipsUsed))
        if (skipsUsed < MAX_SKIPS && pathIdx + 1 < path.size) {
            val afterSkip = consumeLetter(ch, path, pathIdx + 1, skipsUsed + 1)
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
