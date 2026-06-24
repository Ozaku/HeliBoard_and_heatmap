// SPDX-License-Identifier: GPL-3.0-only

// ai-note: v16 — intent path primary alignment; normalized path soft fallback; end neighbor tolerance

package helium314.keyboard.heatmap.swipe

object HeatmapLiteralSwipeScore_v16 {

    private const val EXACT_WEIGHT = 1.0
    private const val SKIP_PATH_WEIGHT = 0.46
    private const val END_MATCH_BOOST = 1.12
    private const val END_NEIGHBOR_BOOST = 1.06
    private const val START_MATCH_BOOST = 1.35
    private const val DWELL_DOUBLE_BOOST = 1.12
    private const val INTENT_EXACT_BOOST = 1.42
    private const val INTENT_PREFIX_BOOST = 1.18
    private const val NORMALIZED_FALLBACK_WEIGHT = 0.52

    fun scoreWord(
        candidate: String,
        pathLetters: List<String>,
        intentPathLetters: List<String> = emptyList(),
        touchedLetters: Set<String>,
        startLabel: String?,
        startDistribution: List<HeatmapKeyLikelihood_v6.LabelWeight>,
        endLabel: String?,
        requireEndMatch: Boolean,
        neighborGraph: HeatmapKeyNeighborGraph_v2.Graph? = null,
        dwellHints: List<HeatmapPathLettersNormalize_v2.DwellHint> = emptyList(),
        kinematics: HeatmapSwipeStrokeKinematics_v1.Result? = null,
    ): Double {
        if (candidate.isEmpty() || touchedLetters.isEmpty()) return Double.NEGATIVE_INFINITY
        val primaryPath = if (intentPathLetters.isNotEmpty()) intentPathLetters else pathLetters
        if (primaryPath.isEmpty()) return Double.NEGATIVE_INFINITY
        val fallbackPath = if (intentPathLetters.isNotEmpty() && pathLetters != intentPathLetters) {
            pathLetters
        } else {
            emptyList()
        }
        if (!HeatmapSwipeWordTouchGate_v2.isAllowed(
                candidate, touchedLetters, startLabel, emptySet(),
            )
        ) {
            return Double.NEGATIVE_INFINITY
        }
        val lower = HeatmapSwipeContractionExpand_v1.lettersOnly(candidate)
        if (lower.isEmpty()) return Double.NEGATIVE_INFINITY
        if (!HeatmapSwipeStartLetterSoftAnchor_v1.wordAllowedAtStart(
                candidate, startDistribution, neighborGraph,
            )
        ) {
            return Double.NEGATIVE_INFINITY
        }
        for (i in 0 until minOf(2, lower.length)) {
            if (i == 0) {
                if (!HeatmapSwipeStartLetterAnchor_v1.isEarlyLetterReachable(
                        neighborGraph, startLabel, lower[i].toString(), i,
                    )
                ) {
                    return Double.NEGATIVE_INFINITY
                }
            }
        }
        if (requireEndMatch && !endLabel.isNullOrEmpty() && lower.length >= 2) {
            if (!HeatmapSwipeEndLetterPolicy_v3.wordEndsOnLift(candidate, endLabel, neighborGraph)) {
                return Double.NEGATIVE_INFINITY
            }
        }
        if (!HeatmapSwipeReachableLetters_v6.isCandidateReachable(
                candidate, primaryPath, requirePathEnd = requireEndMatch, startLabel = startLabel,
            )
        ) {
            return Double.NEGATIVE_INFINITY
        }
        var score = alignScore(lower, primaryPath)
        if (fallbackPath.isNotEmpty()) {
            score = maxOf(score, alignScore(lower, fallbackPath) * NORMALIZED_FALLBACK_WEIGHT)
        }
        if (intentPathLetters.isNotEmpty()) {
            val intentStr = intentPathLetters.joinToString("")
            if (lower == intentStr) score *= INTENT_EXACT_BOOST
            else if (lower.startsWith(intentStr) || intentStr.startsWith(lower)) score *= INTENT_PREFIX_BOOST
        }
        if (!startLabel.isNullOrEmpty() && lower.firstOrNull()?.toString() == startLabel) {
            score *= START_MATCH_BOOST
        }
        if (requireEndMatch && !endLabel.isNullOrEmpty() && lower.length >= 2) {
            when (lower.lastOrNull()?.toString()) {
                endLabel -> score *= END_MATCH_BOOST
                else -> if (neighborGraph != null &&
                    HeatmapKeyNeighborGraph_v2.areNeighbors(
                        neighborGraph, lower.last().toString(), endLabel,
                    )
                ) {
                    score *= END_NEIGHBOR_BOOST
                }
            }
        }
        if (hasDwellDoubleHint(lower, dwellHints, kinematics)) {
            score *= DWELL_DOUBLE_BOOST
        }
        return score
    }

    private fun hasDwellDoubleHint(
        candidate: String,
        dwellHints: List<HeatmapPathLettersNormalize_v2.DwellHint>,
        kinematics: HeatmapSwipeStrokeKinematics_v1.Result?,
    ): Boolean {
        if (dwellHints.isEmpty() || kinematics == null) return false
        for (i in 1 until candidate.length) {
            if (candidate[i] != candidate[i - 1]) continue
            for (dwell in kinematics.dwellSegments) {
                if (dwell.durationMs >= HeatmapSwipeIntentPrototype_v1.dwellMinMs &&
                    dwell.dominantLabel?.firstOrNull() == candidate[i]
                ) {
                    return true
                }
            }
        }
        return false
    }

    private fun alignScore(candidate: String, path: List<String>): Double {
        val len = candidate.length
        var total = 0.0
        var matched = 0.0
        var pathIdx = 0
        var skipsUsed = 0
        val maxSkips = maxSkipsFor(candidate.length, path.size)
        for (i in candidate.indices) {
            val w = positionWeight(i, len)
            total += w
            if (i > 0 && candidate[i] == candidate[i - 1]) {
                matched += w * EXACT_WEIGHT
                continue
            }
            val ch = candidate[i].toString()
            val fit = consumeLetter(ch, path, pathIdx, skipsUsed, maxSkips)
            if (fit != null) {
                matched += w * fit.weight
                pathIdx = fit.nextPathIdx
                skipsUsed = fit.skipsUsed
            }
        }
        if (total <= 0.0) return 0.0
        return matched / total
    }

    private fun maxSkipsFor(candidateLen: Int, pathLen: Int): Int {
        val base = HeatmapSwipeIntentPrototype_v1.maxDictPathSkips
        val extra = (candidateLen - pathLen).coerceAtLeast(0).coerceAtMost(5)
        return (base + extra / 2).coerceAtMost(6)
    }

    private fun positionWeight(index: Int, len: Int): Double = when {
        index == 0 -> HeatmapSwipeIntentPrototype_v1.WEIGHT_FIRST
        index == len - 1 -> HeatmapSwipeIntentPrototype_v1.WEIGHT_LAST
        else -> HeatmapSwipeIntentPrototype_v1.WEIGHT_MIDDLE
    }

    private data class Fit(val weight: Double, val nextPathIdx: Int, val skipsUsed: Int)

    private fun consumeLetter(
        ch: String,
        path: List<String>,
        pathIdx: Int,
        skipsUsed: Int,
        maxSkips: Int,
    ): Fit? {
        if (pathIdx >= path.size) return null
        val options = ArrayList<Fit>(2)
        val exact = findFrom(path, pathIdx, ch) { it == ch }
        if (exact >= 0) options.add(Fit(EXACT_WEIGHT, exact + 1, skipsUsed))
        if (skipsUsed < maxSkips && pathIdx + 1 < path.size) {
            val afterSkip = consumeLetter(ch, path, pathIdx + 1, skipsUsed + 1, maxSkips)
            if (afterSkip != null) {
                options.add(
                    Fit(SKIP_PATH_WEIGHT * afterSkip.weight, afterSkip.nextPathIdx, afterSkip.skipsUsed),
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
