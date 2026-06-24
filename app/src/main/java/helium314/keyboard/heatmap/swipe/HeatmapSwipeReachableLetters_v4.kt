// SPDX-License-Identifier: GPL-3.0-only

// ai-note: Block 3 step 15m — doubles allowed when base letter on path; no dwell requirement

package helium314.keyboard.heatmap.swipe

object HeatmapSwipeReachableLetters_v4 {

    @JvmStatic
    fun allowedPool(pathLetters: List<String>): Set<String> =
        pathLetters.map { it.lowercase() }.toCollection(LinkedHashSet())

    @JvmStatic
    fun isCandidateReachable(
        candidate: String,
        pathLetters: List<String>,
    ): Boolean {
        if (candidate.isEmpty() || pathLetters.isEmpty()) return false
        val lower = HeatmapSwipeContractionExpand_v1.lettersOnly(candidate)
        if (lower.isEmpty()) return false
        if (hasTripleRun(lower)) return false
        val pool = allowedPool(pathLetters)
        return alignExactOnly(lower, pathLetters, pool)
    }

    private fun hasTripleRun(lower: String): Boolean {
        if (lower.length < 3) return false
        for (i in 2 until lower.length) {
            if (lower[i] == lower[i - 1] && lower[i] == lower[i - 2]) return true
        }
        return false
    }

    private fun alignExactOnly(
        candidate: String,
        path: List<String>,
        pool: Set<String>,
    ): Boolean {
        var pathIdx = 0
        var skipsUsed = 0
        val maxSkips = (path.size - 1).coerceAtLeast(1)
        var i = 0
        while (i < candidate.length) {
            val ch = candidate[i].toString()
            if (ch !in pool) return false
            if (i > 0 && candidate[i] == candidate[i - 1]) {
                i++
                continue
            }
            if (pathIdx >= path.size) return false
            if (ch == path[pathIdx]) {
                pathIdx++
                i++
                continue
            }
            if (skipsUsed < maxSkips) {
                skipsUsed++
                pathIdx++
                continue
            }
            return false
        }
        return true
    }
}
