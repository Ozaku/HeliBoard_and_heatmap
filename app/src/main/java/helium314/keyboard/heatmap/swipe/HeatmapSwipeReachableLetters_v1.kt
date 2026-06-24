// SPDX-License-Identifier: GPL-3.0-only

// ai-note: Block 3 step 15i — allowed letter pool = path + immediate neighbors only; blocks fat from f>e>t

package helium314.keyboard.heatmap.swipe

object HeatmapSwipeReachableLetters_v1 {

    @JvmStatic
    fun allowedPool(
        pathLetters: List<String>,
        neighborGraph: HeatmapKeyNeighborGraph_v2.Graph?,
    ): Set<String> {
        val pool = LinkedHashSet<String>()
        for (label in pathLetters) {
            pool.add(label.lowercase())
            pool.addAll(HeatmapKeyNeighborGraph_v2.neighborsOf(neighborGraph, label))
        }
        return pool
    }

    /** ai-note: reject fat on fet — A not in pool; allow feet via doubled e on path e */
    @JvmStatic
    fun isCandidateReachable(
        candidate: String,
        pathLetters: List<String>,
        neighborGraph: HeatmapKeyNeighborGraph_v2.Graph?,
        dwellDoubleLetters: Set<Char> = emptySet(),
    ): Boolean {
        if (candidate.isEmpty()) return false
        val lower = HeatmapSwipeContractionExpand_v1.lettersOnly(candidate)
        val pool = allowedPool(pathLetters, neighborGraph)
        var tripleRun = 0
        for (i in lower.indices) {
            val ch = lower[i]
            if (i > 0 && lower[i] == lower[i - 1]) {
                tripleRun++
                if (tripleRun >= 2) return false
                if (ch.toString() !in pool && ch !in dwellDoubleLetters) return false
                continue
            }
            tripleRun = 0
            if (ch.toString() in pool) continue
            if (ch in dwellDoubleLetters && i > 0 && lower[i - 1] == ch) continue
            return false
        }
        return true
    }
}
