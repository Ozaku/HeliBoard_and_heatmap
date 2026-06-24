// SPDX-License-Identifier: GPL-3.0-only

// ai-note: Block 3 step 15j — strict sequential align; no phantom insert letters (fat blocked on f>e>t)

package helium314.keyboard.heatmap.swipe

object HeatmapSwipeReachableLetters_v2 {

    @JvmStatic
    fun allowedPool(
        pathLetters: List<String>,
        neighborGraph: HeatmapKeyNeighborGraph_v2.Graph?,
    ): Set<String> = HeatmapSwipeReachableLetters_v1.allowedPool(pathLetters, neighborGraph)

    @JvmStatic
    fun isCandidateReachable(
        candidate: String,
        pathLetters: List<String>,
        neighborGraph: HeatmapKeyNeighborGraph_v2.Graph?,
        dwellDoubleLetters: Set<Char> = emptySet(),
    ): Boolean {
        if (candidate.isEmpty() || pathLetters.isEmpty()) return false
        val lower = HeatmapSwipeContractionExpand_v1.lettersOnly(candidate)
        if (lower.isEmpty()) return false
        if (hasTripleRun(lower)) return false
        return alignStrict(lower, pathLetters, neighborGraph, dwellDoubleLetters)
    }

    private fun hasTripleRun(lower: String): Boolean {
        if (lower.length < 3) return false
        for (i in 2 until lower.length) {
            if (lower[i] == lower[i - 1] && lower[i] == lower[i - 2]) return true
        }
        return false
    }

    private fun alignStrict(
        candidate: String,
        path: List<String>,
        neighborGraph: HeatmapKeyNeighborGraph_v2.Graph?,
        dwellDoubleLetters: Set<Char>,
    ): Boolean {
        var pathIdx = 0
        var skipsUsed = 0
        val maxSkips = (path.size - 1).coerceAtLeast(1)
        var i = 0
        while (i < candidate.length) {
            val ch = candidate[i].toString()
            if (i > 0 && candidate[i] == candidate[i - 1]) {
                val letter = candidate[i]
                if (letter in dwellDoubleLetters) {
                    i++
                    continue
                }
                val pathLetter = path.getOrNull(pathIdx - 1)?.firstOrNull()
                if (pathLetter == letter) {
                    i++
                    continue
                }
                return false
            }
            if (pathIdx >= path.size) return false
            val pathLetter = path[pathIdx]
            if (ch == pathLetter || HeatmapKeyNeighborGraph_v2.areNeighbors(neighborGraph, ch, pathLetter)) {
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
