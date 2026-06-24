// SPDX-License-Identifier: GPL-3.0-only

// ai-note: Block 3 step 15j — drop spurious middle beat labels (F>A>E>T → F>E>T when A not on stroke chain)

package helium314.keyboard.heatmap.swipe

object HeatmapSwipePathChainFilter_v1 {

    @JvmStatic
    fun filterSpuriousMiddleKeys(
        raw: List<String>,
        neighborGraph: HeatmapKeyNeighborGraph_v2.Graph?,
    ): List<String> {
        if (raw.size < 3) return raw
        val out = ArrayList<String>(raw.size)
        out.add(raw[0])
        var i = 1
        while (i < raw.lastIndex) {
            val prev = out.last()
            val cur = raw[i]
            val next = raw[i + 1]
            when {
                cur == prev -> {
                    out.add(cur)
                    i++
                }
                HeatmapKeyNeighborGraph_v2.areNeighbors(neighborGraph, prev, cur) -> {
                    out.add(cur)
                    i++
                }
                shouldDropSpuriousMiddle(prev, cur, next, neighborGraph) -> i++
                else -> {
                    out.add(cur)
                    i++
                }
            }
        }
        out.add(raw.last())
        return HeatmapPathLettersNormalize_v1.collapseConsecutiveDuplicates(out)
    }

    private fun shouldDropSpuriousMiddle(
        prev: String,
        mid: String,
        next: String,
        neighborGraph: HeatmapKeyNeighborGraph_v2.Graph?,
    ): Boolean {
        if (mid == next || prev == mid) return false
        if (HeatmapKeyNeighborGraph_v2.areNeighbors(neighborGraph, prev, mid)) return false
        if (isBridgeLetter(prev, mid, next, neighborGraph)) return true
        return HeatmapKeyNeighborGraph_v2.areNeighbors(neighborGraph, prev, next)
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
}
