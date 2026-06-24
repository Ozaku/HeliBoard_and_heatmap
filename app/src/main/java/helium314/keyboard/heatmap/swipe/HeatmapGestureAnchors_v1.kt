// SPDX-License-Identifier: GPL-3.0-only

// ai-note: v1 — extracts the gesture's INTENDED-letter anchors: sharp corners, sustained
// dwells/pauses, and the two endpoints, mapped to their nearest letter key (deduped in order).
// These are the letters a candidate word MUST cover; transit keys swept fast and straight
// (the classic 'i' on the top row) are deliberately excluded. Used by HeatmapGestureShapeScore_v1
// to penalize words that miss real corners/pauses (e.g. "nine" for "now" misses the o-corner and
// w-endpoint). Validated offline on pulled 0.0.0.52/0.0.0.53 traces (data_pull/shark2_harness_v2.py).
package helium314.keyboard.heatmap.swipe

import kotlin.math.acos
import kotlin.math.hypot

object HeatmapGestureAnchors_v1 {

    /**
     * Ordered list of anchor letters (corners + dwells + start/end), deduped consecutively.
     * [points] are raw [x,y] samples; [timesMs] are matching timestamps (relative ms); both
     * must be the same length. Returns lowercase letters.
     */
    fun extract(
        points: List<DoubleArray>,
        timesMs: IntArray,
        keyModel: HeatmapGestureKeyModel_v1,
    ): List<Char> = dedupeToKeys(anchorIndices(points, timesMs, keyModel), points, keyModel)

    /**
     * The raw sample indices flagged as intended-letter anchors (corners + sustained dwells + the
     * two endpoints), sorted ascending. Exposed so exported geometry (HeatmapSwipeGeometryVector_v2)
     * can reuse the SAME validated detector the live decoder uses, instead of the legacy 16-degree
     * corner detector that over-detects.
     */
    fun anchorIndices(
        points: List<DoubleArray>,
        timesMs: IntArray,
        keyModel: HeatmapGestureKeyModel_v1,
    ): List<Int> {
        val n = points.size
        if (n == 0) return emptyList()
        if (n < 3) return (0 until n).toList()
        val c = HeatmapGestureTuningConstants_v1
        val radius = keyModel.radius.coerceAtLeast(1.0)
        val minWin = radius * c.ANCHOR_WINDOW_MIN_DIST_FRAC
        val dwellRadius = radius * c.ANCHOR_DWELL_RADIUS_FRAC

        val flagged = sortedSetOf(0, n - 1)

        // Corners: turning angle measured over a jitter-resistant window on each side.
        for (i in 1 until n - 1) {
            val prev = walkBack(points, i, minWin)
            val next = walkForward(points, i, minWin)
            if (prev < 0 || next < 0) continue
            val ax = points[i][0] - points[prev][0]
            val ay = points[i][1] - points[prev][1]
            val bx = points[next][0] - points[i][0]
            val by = points[next][1] - points[i][1]
            val na = hypot(ax, ay)
            val nb = hypot(bx, by)
            if (na < 1e-6 || nb < 1e-6) continue
            val cos = ((ax * bx + ay * by) / (na * nb)).coerceIn(-1.0, 1.0)
            val turn = acos(cos)
            if (turn >= c.ANCHOR_CORNER_MIN_RAD) flagged.add(i)
        }

        // Dwells: a run of samples staying within dwellRadius that spans >= ANCHOR_DWELL_MIN_MS.
        var i = 0
        while (i < n) {
            var j = i + 1
            while (j < n &&
                hypot(points[j][0] - points[i][0], points[j][1] - points[i][1]) <= dwellRadius
            ) {
                j++
            }
            val dt = timesMs[minOf(j, n - 1)] - timesMs[i]
            if (j - i >= 2 && dt >= c.ANCHOR_DWELL_MIN_MS) {
                flagged.add((i + j - 1) / 2)
            }
            i = if (j > i + 1) j else i + 1
        }

        return flagged.toList()
    }

    /** Fraction of [anchors] found in [word] as an in-order subsequence (exact letter match). */
    fun coverage(word: String, anchors: List<Char>): Double {
        if (anchors.isEmpty()) return 1.0
        var j = 0
        var matched = 0
        for (raw in word) {
            val ch = raw.lowercaseChar()
            if (j < anchors.size && ch == anchors[j]) {
                matched++
                j++
            }
        }
        return matched.toDouble() / anchors.size
    }

    private fun dedupeToKeys(
        indices: List<Int>,
        points: List<DoubleArray>,
        keyModel: HeatmapGestureKeyModel_v1,
    ): List<Char> {
        val out = ArrayList<Char>(indices.size)
        var last: Char? = null
        for (idx in indices) {
            val ch = keyModel.classify(points[idx][0], points[idx][1]) ?: continue
            if (ch != last) {
                out.add(ch)
                last = ch
            }
        }
        return out
    }

    private fun walkBack(points: List<DoubleArray>, i: Int, minDist: Double): Int {
        var k = i - 1
        while (k > 0 && hypot(points[i][0] - points[k][0], points[i][1] - points[k][1]) < minDist) {
            k--
        }
        return k
    }

    private fun walkForward(points: List<DoubleArray>, i: Int, minDist: Double): Int {
        var k = i + 1
        val last = points.size - 1
        while (k < last &&
            hypot(points[k][0] - points[i][0], points[k][1] - points[i][1]) < minDist
        ) {
            k++
        }
        return k
    }
}
