// SPDX-License-Identifier: GPL-3.0-only

// ai-note: Block 3 step 15b — collapse consecutive duplicate key labels from slow precise swipes

package helium314.keyboard.heatmap.swipe

object HeatmapPathLettersNormalize_v1 {

    /** ai-note: plattess→plates, theere→there; preserves intentional loops only when non-adjacent */
    @JvmStatic
    fun collapseConsecutiveDuplicates(labels: List<String>): List<String> {
        if (labels.isEmpty()) return labels
        val out = ArrayList<String>(labels.size)
        var prev: String? = null
        for (label in labels) {
            if (label == prev) continue
            out.add(label)
            prev = label
        }
        return out
    }
}
