// SPDX-License-Identifier: GPL-3.0-only

// ai-note: v1 — letter-key geometry model for the gesture-template decoder.
// Wraps a HeatmapCoordinateMap_v1.Snapshot into fast lookups: per-letter key center,
// inter-key spacing + radius, neighbor sets, and nearest-key classification for a point.
// All coordinates are keyboard-local pixels (same space as InputPointers).
package helium314.keyboard.heatmap.swipe

import helium314.keyboard.heatmap.learning.HeatmapCoordinateMap_v1
import kotlin.math.hypot
import kotlin.math.min

class HeatmapGestureKeyModel_v1 private constructor(
    val centers: Map<Char, DoubleArray>,
    val spacing: Double,
    val radius: Double,
    val neighbors: Map<Char, Set<Char>>,
    private val entries: List<HeatmapCoordinateMap_v1.KeyBoundsEntry>,
) {

    fun centerFor(ch: Char): DoubleArray? = centers[ch.lowercaseChar()]

    fun neighborsOf(ch: Char): Set<Char> = neighbors[ch.lowercaseChar()] ?: emptySet()

    /** Letter key whose hit box contains (x,y), else the nearest key center. */
    fun classify(x: Double, y: Double): Char? {
        var best: Char? = null
        var bestD = Double.MAX_VALUE
        for (e in entries) {
            if (x >= e.left && x < e.right && y >= e.top && y < e.bottom) {
                return e.storageLabel.firstOrNull()
            }
            val cx = (e.left + e.right) / 2.0
            val cy = (e.top + e.bottom) / 2.0
            val d = hypot(cx - x, cy - y)
            if (d < bestD) {
                bestD = d
                best = e.storageLabel.firstOrNull()
            }
        }
        return best
    }

    companion object {
        fun from(snapshot: HeatmapCoordinateMap_v1.Snapshot): HeatmapGestureKeyModel_v1 {
            val entries = snapshot.keys.filter {
                it.storageLabel.length == 1 && it.storageLabel[0].isLetter()
            }
            val centers = HashMap<Char, DoubleArray>(entries.size * 2)
            for (e in entries) {
                val c = e.storageLabel[0]
                val dx = helium314.keyboard.heatmap.learning.HeatmapUserProfile_v1.getOffsetX(c).toDouble()
                val dy = helium314.keyboard.heatmap.learning.HeatmapUserProfile_v1.getOffsetY(c).toDouble()
                centers[c] = doubleArrayOf(e.centerX.toDouble() + dx, e.centerY.toDouble() + dy)
            }
            val spacing = estimateSpacing(centers)
            val reach = spacing * HeatmapGestureTuningConstants_v1.NEIGHBOR_FACTOR
            val neighbors = HashMap<Char, Set<Char>>(centers.size * 2)
            for ((c, p) in centers) {
                val set = LinkedHashSet<Char>()
                for ((o, q) in centers) {
                    if (o == c) continue
                    if (hypot(p[0] - q[0], p[1] - q[1]) <= reach) set.add(o)
                }
                neighbors[c] = set
            }
            return HeatmapGestureKeyModel_v1(centers, spacing, spacing / 2.0, neighbors, entries)
        }

        private fun estimateSpacing(centers: Map<Char, DoubleArray>): Double {
            if (centers.size < 2) return 100.0
            val mins = ArrayList<Double>(centers.size)
            for ((c, p) in centers) {
                var best = Double.MAX_VALUE
                for ((o, q) in centers) {
                    if (o == c) continue
                    val d = hypot(p[0] - q[0], p[1] - q[1])
                    if (d < best) best = d
                }
                if (best < Double.MAX_VALUE) mins.add(best)
            }
            mins.sort()
            return if (mins.isEmpty()) 100.0 else mins[mins.size / 2].coerceAtLeast(1.0)
        }
    }
}
