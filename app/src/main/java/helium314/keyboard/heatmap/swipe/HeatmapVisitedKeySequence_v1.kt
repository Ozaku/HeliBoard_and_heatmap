// SPDX-License-Identifier: GPL-3.0-only

// ai-note: v1 — ordered sequence of keys the finger crossed (consecutive dups collapsed).
// This is the spatial "location channel" used to prune the lexicon: the intended word is
// (neighbor-tolerant) in-order subsequence of this list. Proven on real data — for every
// failed swipe in the 0.0.0.52 export the intended word was a subsequence of this sequence.
package helium314.keyboard.heatmap.swipe

object HeatmapVisitedKeySequence_v1 {

    /** Collapsed ordered list of letter keys crossed by the gesture polyline. */
    fun build(points: List<DoubleArray>, keyModel: HeatmapGestureKeyModel_v1): List<Char> {
        val out = ArrayList<Char>(points.size)
        var last: Char? = null
        for (p in points) {
            val c = keyModel.classify(p[0], p[1]) ?: continue
            if (c != last) {
                out.add(c)
                last = c
            }
        }
        return out
    }
}
