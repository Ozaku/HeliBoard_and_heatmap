// SPDX-License-Identifier: GPL-3.0-only

// ai-note: Block 3 step 15o — allow f,e,e,t when stroke order is f,e,t (double at visited key)

package helium314.keyboard.heatmap.swipe

object HeatmapSwipeStrokeOrderPath_v2 {

    @JvmStatic
    fun filterToStrokeOrder(path: List<String>, strokeOrder: List<String>): List<String> {
        if (path.isEmpty() || strokeOrder.isEmpty()) return emptyList()
        val out = ArrayList<String>(path.size)
        var orderIdx = 0
        for (letter in path) {
            if (out.isNotEmpty() && out.last() == letter) {
                out.add(letter)
                continue
            }
            while (orderIdx < strokeOrder.size && strokeOrder[orderIdx] != letter) {
                orderIdx++
            }
            if (orderIdx >= strokeOrder.size) return emptyList()
            out.add(letter)
            orderIdx++
        }
        return out
    }
}
