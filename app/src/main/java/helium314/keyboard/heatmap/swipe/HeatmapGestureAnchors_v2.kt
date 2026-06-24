// SPDX-License-Identifier: GPL-3.0-only

// ai-note: v2 — the gesture's INTENDED-letter anchors, now the PRIMARY decoder signal (not a weak
// tie-breaker like v1). Detects, in path order:
//   START  = first sample
//   STOP   = a hard stop: a local speed minimum dipping below STOP_SPEED_FRAC * mean speed
//            (catches brief stops the time-based dwell detector missed — the user's #1 complaint)
//   CORNER = a sharp turn (>= ANCHOR_CORNER_MIN_RAD ~63deg) measured over a jitter-resistant window
//   END    = last sample
// Each flagged index is mapped to its nearest letter key; consecutive same-key anchors collapse.
// The resulting key sequence drives strict anchor-aligned candidate generation; the rich Anchor
// metadata (type/index/x/y/key/turnAngle/speed/dwellMs) is exported for offline threshold tuning.
// AI EDIT MAP:
//   extract()      -> Result(anchors, keys): ordered anchors + deduped key sequence
//   keysOnly()     -> just the deduped anchor key chars (candidate-gen input)
//   detectStops()  -> velocity-minima hard-stop indices
//   detectCorners()-> sharp-turn indices (reused logic from v1)
package helium314.keyboard.heatmap.swipe

import kotlin.math.acos
import kotlin.math.hypot

object HeatmapGestureAnchors_v2 {

    enum class Type { START, STOP, CORNER, END }

    data class Anchor(
        val type: Type,
        val index: Int,
        val x: Double,
        val y: Double,
        val key: Char?,
        val turnAngleRad: Double,
        val localSpeedPxPerSec: Double,
        val dwellMs: Int,
    )

    data class Result(val anchors: List<Anchor>, val keys: List<Char>)

    /** Ordered anchors + the deduped lowercase key sequence they map to. */
    fun extract(
        points: List<DoubleArray>,
        timesMs: IntArray,
        keyModel: HeatmapGestureKeyModel_v1,
    ): Result {
        val n = points.size
        if (n == 0) return Result(emptyList(), emptyList())
        val c = HeatmapGestureTuningConstants_v2
        val radius = keyModel.radius.coerceAtLeast(1.0)

        val speeds = pointSpeeds(points, timesMs)
        val cornerSet = if (n >= 3) detectCorners(points, radius, c) else emptyMap()
        val stopSet = if (n >= 3) detectStops(points, speeds, radius, c) else emptySet()

        // Flag indices with a type priority (endpoints win, then corner, then stop).
        val flaggedType = sortedMapOf<Int, Type>()
        flaggedType[0] = Type.START
        flaggedType[n - 1] = Type.END
        for (i in stopSet) flaggedType.putIfAbsent(i, Type.STOP)
        for ((i, _) in cornerSet) {
            val prev = flaggedType[i]
            if (prev == null || prev == Type.STOP) flaggedType[i] = Type.CORNER
        }
        // Endpoints always keep their type even if also a corner/stop.
        flaggedType[0] = Type.START
        flaggedType[n - 1] = Type.END

        val anchors = ArrayList<Anchor>(flaggedType.size)
        var lastKey: Char? = null
        for ((idx, type) in flaggedType) {
            val key = keyModel.classify(points[idx][0], points[idx][1])
            // Collapse consecutive same-key anchors, but never drop the START or END rows.
            if (key != null && key == lastKey && type != Type.START && type != Type.END) continue
            anchors.add(
                Anchor(
                    type = type,
                    index = idx,
                    x = points[idx][0],
                    y = points[idx][1],
                    key = key,
                    turnAngleRad = cornerSet[idx] ?: 0.0,
                    localSpeedPxPerSec = speeds.getOrElse(idx) { 0.0 } * 1000.0,
                    dwellMs = 0,
                ),
            )
            if (key != null) lastKey = key
        }
        val keys = dedupeKeys(anchors)
        return Result(anchors, keys)
    }

    fun keysOnly(
        points: List<DoubleArray>,
        timesMs: IntArray,
        keyModel: HeatmapGestureKeyModel_v1,
    ): List<Char> = extract(points, timesMs, keyModel).keys

    private fun dedupeKeys(anchors: List<Anchor>): List<Char> {
        val out = ArrayList<Char>(anchors.size)
        var last: Char? = null
        for (a in anchors) {
            val k = a.key ?: continue
            if (k != last) {
                out.add(k)
                last = k
            }
        }
        return out
    }

    /** Per-point speed in px/ms (index 0 = 0). Smoothed by a small half-window to absorb 0-dt batching. */
    private fun pointSpeeds(points: List<DoubleArray>, timesMs: IntArray): DoubleArray {
        val n = points.size
        val sm = DoubleArray(n)
        val half = HeatmapGestureTuningConstants_v2.STOP_SPEED_SMOOTH_HALF
        for (i in 0 until n) {
            val start = maxOf(0, i - half)
            val end = minOf(n - 1, i + half)
            val dt = (timesMs[end] - timesMs[start]).coerceAtLeast(1)
            var dist = 0.0
            for (k in start until end) {
                dist += hypot(points[k + 1][0] - points[k][0], points[k + 1][1] - points[k][1])
            }
            sm[i] = dist / dt
        }
        return sm
    }

    /** Local speed minima below STOP_SPEED_FRAC * mean speed, separated by STOP_MIN_SEPARATION. */
    private fun detectStops(
        points: List<DoubleArray>,
        speeds: DoubleArray,
        radius: Double,
        c: HeatmapGestureTuningConstants_v2,
    ): Set<Int> {
        val n = points.size
        var sum = 0.0
        var cnt = 0
        for (i in 1 until n) { sum += speeds[i]; cnt++ }
        val mean = if (cnt > 0) sum / cnt else 0.0
        if (mean <= 0.0) return emptySet()
        val threshold = c.STOP_SPEED_FRAC * mean
        val minSep = radius * c.STOP_MIN_SEPARATION_FRAC
        val out = LinkedHashSet<Int>()
        var lastStopXY: DoubleArray? = null
        var i = 1
        while (i < n - 1) {
            val isLocalMin = speeds[i] <= speeds[i - 1] && speeds[i] <= speeds[i + 1]
            if (speeds[i] < threshold && isLocalMin) {
                // Walk to the slowest sample in this slow run.
                var j = i
                var bestIdx = i
                while (j < n - 1 && speeds[j] < threshold) {
                    if (speeds[j] < speeds[bestIdx]) bestIdx = j
                    j++
                }
                val prevXY = lastStopXY
                val far = prevXY == null ||
                    hypot(points[bestIdx][0] - prevXY[0], points[bestIdx][1] - prevXY[1]) >= minSep
                if (far) {
                    out.add(bestIdx)
                    lastStopXY = points[bestIdx]
                }
                i = j + 1
            } else {
                i++
            }
        }
        return out
    }

    /** Sharp-turn vertices -> turn angle (radians), keyed by sample index. */
    private fun detectCorners(
        points: List<DoubleArray>,
        radius: Double,
        c: HeatmapGestureTuningConstants_v2,
    ): Map<Int, Double> {
        val n = points.size
        val minWin = radius * c.ANCHOR_WINDOW_MIN_DIST_FRAC
        val rawCorners = HashMap<Int, Double>()
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
            if (turn >= c.ANCHOR_CORNER_MIN_RAD) rawCorners[i] = turn
        }

        // Cluster consecutive/nearby corners (avoids double-counting on slow, rounded physical turns)
        val out = HashMap<Int, Double>()
        val clusterSep = radius * c.CORNER_CLUSTER_MAX_SEPARATION_FRAC
        val sortedIndices = rawCorners.keys.sorted()
        var i = 0
        while (i < sortedIndices.size) {
            var j = i
            var bestIdx = sortedIndices[i]
            var maxTurn = rawCorners[bestIdx]!!
            while (j + 1 < sortedIndices.size) {
                val nextIdx = sortedIndices[j + 1]
                val dist = hypot(points[nextIdx][0] - points[bestIdx][0], points[nextIdx][1] - points[bestIdx][1])
                if (dist <= clusterSep || nextIdx - sortedIndices[j] <= 2) {
                    j++
                    val turn = rawCorners[nextIdx]!!
                    if (turn > maxTurn) {
                        maxTurn = turn
                        bestIdx = nextIdx
                    }
                } else {
                    break
                }
            }
            out[bestIdx] = maxTurn
            i = j + 1
        }
        return out
    }

    private fun walkBack(points: List<DoubleArray>, i: Int, minDist: Double): Int {
        var k = i - 1
        while (k > 0 && hypot(points[i][0] - points[k][0], points[i][1] - points[k][1]) < minDist) k--
        return k
    }

    private fun walkForward(points: List<DoubleArray>, i: Int, minDist: Double): Int {
        var k = i + 1
        val last = points.size - 1
        while (k < last && hypot(points[k][0] - points[i][0], points[k][1] - points[i][1]) < minDist) k++
        return k
    }
}
