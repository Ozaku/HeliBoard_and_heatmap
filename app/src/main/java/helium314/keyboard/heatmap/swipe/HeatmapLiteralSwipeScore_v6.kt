// SPDX-License-Identifier: GPL-3.0-only

// ai-note: Block 3 step 15g — ordered letter match: exact > neighbor > small insert/skip; lift end strict

package helium314.keyboard.heatmap.swipe

object HeatmapLiteralSwipeScore_v6 {

    private const val EXACT_WEIGHT = 1.0
    private const val NEIGHBOR_WEIGHT = 0.62
    private const val INSERT_WEIGHT = 0.72
    private const val SKIP_PATH_WEIGHT = 0.5
    private const val MAX_INSERTS = 2
    private const val MAX_SKIPS = 2

    fun scoreWord(
        candidate: String,
        pathLetters: List<String>,
        startLabel: String?,
        endLabel: String?,
        neighborGraph: HeatmapKeyNeighborGraph_v2.Graph? = null,
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
        return alignScore(lower, pathLetters, neighborGraph)
    }

    private fun alignScore(
        candidate: String,
        path: List<String>,
        neighborGraph: HeatmapKeyNeighborGraph_v2.Graph?,
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
