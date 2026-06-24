// SPDX-License-Identifier: GPL-3.0-only

// ai-note: v1 — polyline resampling + arc length + shape normalization for SHARK2 matching.
// Resamples any polyline to N points equidistant by arc length so the location channel can
// compare gesture vs word template point-for-point regardless of raw sample density.
package helium314.keyboard.heatmap.swipe

import kotlin.math.hypot

object HeatmapGestureResample_v1 {

    /** Resample [pts] (each [x,y]) to [n] points equidistant by arc length. */
    fun resample(pts: List<DoubleArray>, n: Int): Array<DoubleArray> {
        if (pts.isEmpty()) return Array(n) { doubleArrayOf(0.0, 0.0) }
        if (pts.size == 1) return Array(n) { pts[0].copyOf() }
        val cum = DoubleArray(pts.size)
        for (i in 1 until pts.size) {
            cum[i] = cum[i - 1] + hypot(pts[i][0] - pts[i - 1][0], pts[i][1] - pts[i - 1][1])
        }
        val total = cum[cum.size - 1]
        if (total <= 1e-6) return Array(n) { pts[0].copyOf() }
        val out = Array(n) { DoubleArray(2) }
        var j = 0
        for (i in 0 until n) {
            val t = total * i / (n - 1)
            while (j < cum.size - 2 && cum[j + 1] < t) j++
            val span = cum[j + 1] - cum[j]
            val f = if (span <= 1e-9) 0.0 else (t - cum[j]) / span
            out[i][0] = pts[j][0] + f * (pts[j + 1][0] - pts[j][0])
            out[i][1] = pts[j][1] + f * (pts[j + 1][1] - pts[j][1])
        }
        return out
    }

    fun arcLength(pts: List<DoubleArray>): Double {
        if (pts.size < 2) return 0.0
        var s = 0.0
        for (i in 1 until pts.size) {
            s += hypot(pts[i][0] - pts[i - 1][0], pts[i][1] - pts[i - 1][1])
        }
        return s
    }

    /** Translate to centroid and scale by bounding-box diagonal (shape channel input). */
    fun shapeNormalize(rs: Array<DoubleArray>): Array<DoubleArray> {
        var cx = 0.0
        var cy = 0.0
        for (p in rs) {
            cx += p[0]; cy += p[1]
        }
        cx /= rs.size; cy /= rs.size
        var minX = Double.MAX_VALUE
        var minY = Double.MAX_VALUE
        var maxX = -Double.MAX_VALUE
        var maxY = -Double.MAX_VALUE
        for (p in rs) {
            val x = p[0] - cx
            val y = p[1] - cy
            if (x < minX) minX = x
            if (x > maxX) maxX = x
            if (y < minY) minY = y
            if (y > maxY) maxY = y
        }
        val diag = hypot(maxX - minX, maxY - minY)
        val out = Array(rs.size) { DoubleArray(2) }
        if (diag < 1e-6) {
            for (i in rs.indices) {
                out[i][0] = rs[i][0] - cx
                out[i][1] = rs[i][1] - cy
            }
            return out
        }
        for (i in rs.indices) {
            out[i][0] = (rs[i][0] - cx) / diag
            out[i][1] = (rs[i][1] - cy) / diag
        }
        return out
    }
}
