// SPDX-License-Identifier: GPL-3.0-only

// ai-note: v17 — strict monotonic stroke-order alignment; no path skips or visit fallback

package helium314.keyboard.heatmap.swipe

object HeatmapLiteralSwipeScore_v17 {

    private const val EXACT_WEIGHT = 1.0
    private const val END_MATCH_BOOST = 1.12
    private const val END_NEIGHBOR_BOOST = 1.06
    private const val START_MATCH_BOOST = 1.35
    private const val DWELL_DOUBLE_BOOST = 1.12
    private const val EXACT_PATH_BOOST = 1.45

    fun scoreWord(
        candidate: String,
        orderedPath: List<String>,
        touchedLetters: Set<String>,
        startLabel: String?,
        startDistribution: List<HeatmapKeyLikelihood_v6.LabelWeight>,
        endLabel: String?,
        requireEndMatch: Boolean,
        neighborGraph: HeatmapKeyNeighborGraph_v2.Graph? = null,
        dwellHints: List<HeatmapPathLettersNormalize_v2.DwellHint> = emptyList(),
        kinematics: HeatmapSwipeStrokeKinematics_v1.Result? = null,
    ): Double {
        if (candidate.isEmpty() || orderedPath.isEmpty() || touchedLetters.isEmpty()) {
            return Double.NEGATIVE_INFINITY
        }
        if (!HeatmapSwipeStrokeMonotonicPath_v1.isMonotonicSubsequence(candidate, orderedPath)) {
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
        if (!HeatmapSwipeStartLetterSoftAnchor_v1.wordAllowedAtStart(
                candidate, startDistribution, neighborGraph,
            )
        ) {
            return Double.NEGATIVE_INFINITY
        }
        if (requireEndMatch && !endLabel.isNullOrEmpty() && lower.length >= 2) {
            if (!HeatmapSwipeEndLetterPolicy_v3.wordEndsOnLift(candidate, endLabel, neighborGraph)) {
                return Double.NEGATIVE_INFINITY
            }
        }
        var score = alignScoreMonotonic(lower, orderedPath)
        val pathStr = orderedPath.joinToString("")
        if (lower == pathStr) score *= EXACT_PATH_BOOST
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

    private fun alignScoreMonotonic(candidate: String, path: List<String>): Double {
        val len = candidate.length
        var total = 0.0
        var matched = 0.0
        var pathIdx = 0
        for (i in candidate.indices) {
            val w = positionWeight(i, len)
            total += w
            if (i > 0 && candidate[i] == candidate[i - 1]) {
                matched += w * EXACT_WEIGHT
                continue
            }
            val ch = candidate[i].toString()
            var found = false
            for (j in pathIdx until path.size) {
                if (path[j].equals(ch, ignoreCase = true)) {
                    matched += w * EXACT_WEIGHT
                    pathIdx = j + 1
                    found = true
                    break
                }
            }
            if (!found) return 0.0
        }
        if (total <= 0.0) return 0.0
        return matched / total
    }

    private fun positionWeight(index: Int, len: Int): Double = when {
        index == 0 -> HeatmapSwipeIntentPrototype_v1.WEIGHT_FIRST
        index == len - 1 -> HeatmapSwipeIntentPrototype_v1.WEIGHT_LAST
        else -> HeatmapSwipeIntentPrototype_v1.WEIGHT_MIDDLE
    }
}