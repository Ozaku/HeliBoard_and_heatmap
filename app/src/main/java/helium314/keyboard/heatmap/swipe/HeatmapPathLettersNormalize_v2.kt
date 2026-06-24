// SPDX-License-Identifier: GPL-3.0-only

// ai-note: Block 3 step 15i — bridge collapse e,r,t→e,t; dwell run detection for double letters

package helium314.keyboard.heatmap.swipe

object HeatmapPathLettersNormalize_v2 {

    data class DwellHint(
        val dedupedIndex: Int,
        val letter: String,
        val rawRunLength: Int,
    )

    data class Normalized(
        val letters: List<String>,
        val dwellHints: List<DwellHint>,
    )

    @JvmStatic
    fun normalize(
        raw: List<String>,
        neighborGraph: HeatmapKeyNeighborGraph_v2.Graph?,
    ): Normalized {
        val deduped = HeatmapPathLettersNormalize_v1.collapseConsecutiveDuplicates(raw)
        val bridged = collapseBridgeMiddleKeys(deduped, neighborGraph)
        val dwellHints = detectDwellRuns(raw, bridged)
        return Normalized(letters = bridged, dwellHints = dwellHints)
    }

    /** ai-note: E—R—T straight pass: R is bridge between E and T, drop it */
    @JvmStatic
    fun collapseBridgeMiddleKeys(
        path: List<String>,
        neighborGraph: HeatmapKeyNeighborGraph_v2.Graph?,
    ): List<String> {
        if (path.size < 3) return path
        val out = ArrayList<String>(path.size)
        out.add(path[0])
        for (i in 1 until path.lastIndex) {
            val prev = out.last()
            val mid = path[i]
            val next = path[i + 1]
            if (isBridgeLetter(prev, mid, next, neighborGraph)) continue
            out.add(mid)
        }
        out.add(path.last())
        return HeatmapPathLettersNormalize_v1.collapseConsecutiveDuplicates(out)
    }

    private fun isBridgeLetter(
        prev: String,
        mid: String,
        next: String,
        neighborGraph: HeatmapKeyNeighborGraph_v2.Graph?,
    ): Boolean {
        if (prev == mid || mid == next) return false
        val touchesPrev = HeatmapKeyNeighborGraph_v2.areNeighbors(neighborGraph, prev, mid)
        val touchesNext = HeatmapKeyNeighborGraph_v2.areNeighbors(neighborGraph, mid, next)
        val prevToNext = HeatmapKeyNeighborGraph_v2.areNeighbors(neighborGraph, prev, next)
        return touchesPrev && touchesNext && !prevToNext
    }

    @JvmStatic
    fun detectDwellRuns(raw: List<String>, deduped: List<String>): List<DwellHint> {
        if (raw.isEmpty() || deduped.isEmpty()) return emptyList()
        val hints = ArrayList<DwellHint>()
        var i = 0
        while (i < raw.size) {
            val letter = raw[i]
            var run = 1
            while (i + run < raw.size && raw[i + run] == letter) run++
            if (run >= 2) {
                val dedupIdx = mapRawIndexToDeduped(raw, deduped, i)
                if (dedupIdx >= 0) {
                    hints.add(DwellHint(dedupIdx, letter, run))
                }
            }
            i += run
        }
        return hints
    }

    private fun mapRawIndexToDeduped(raw: List<String>, deduped: List<String>, rawIndex: Int): Int {
        var dedupPtr = 0
        var prev: String? = null
        for (i in 0..rawIndex.coerceAtMost(raw.lastIndex)) {
            if (raw[i] != prev) {
                if (dedupPtr < deduped.size && deduped[dedupPtr] == raw[i]) dedupPtr++
                prev = raw[i]
            }
        }
        return (dedupPtr - 1).coerceAtLeast(0)
    }
}
