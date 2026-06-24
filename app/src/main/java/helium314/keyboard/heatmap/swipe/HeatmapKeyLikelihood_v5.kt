// SPDX-License-Identifier: GPL-3.0-only

// ai-note: Block 3 step 15k — HARD RULE: letter only if stroke on key OR miss ≤25% of key length; else null

package helium314.keyboard.heatmap.swipe

import helium314.keyboard.heatmap.learning.HeatmapCoordinateMap_v1
import kotlin.math.sqrt

object HeatmapKeyLikelihood_v5 {

    data class ScoredKey(
        val key: HeatmapCoordinateMap_v1.KeyBoundsEntry,
        val likelihood: Double,
    )

    /** ai-note: user rule — max miss beyond key edge = 25% of key length (min dimension) */
    const val MAX_MISS_FACTOR = 0.25

    private const val MIN_DOMINANCE_RATIO = 2.5

    @JvmStatic
    fun maxMissPx(key: HeatmapCoordinateMap_v1.KeyBoundsEntry): Double {
        val keyWidth = (key.right - key.left).coerceAtLeast(1)
        val keyHeight = (key.bottom - key.top).coerceAtLeast(1)
        return minOf(keyWidth, keyHeight) * MAX_MISS_FACTOR
    }

    @JvmStatic
    fun isWithinHitZone(
        layout: HeatmapCoordinateMap_v1.Snapshot,
        x: Int,
        y: Int,
        key: HeatmapCoordinateMap_v1.KeyBoundsEntry,
    ): Boolean = distanceToBox(x, y, key) <= maxMissPx(key)

    @JvmStatic
    fun likelihoodAt(
        layout: HeatmapCoordinateMap_v1.Snapshot,
        x: Int,
        y: Int,
        key: HeatmapCoordinateMap_v1.KeyBoundsEntry,
    ): Double {
        val dist = distanceToBox(x, y, key)
        val maxMiss = maxMissPx(key)
        if (dist > maxMiss) return 0.0
        if (dist <= 0.0) return 1.0
        return 1.0 - (dist / maxMiss)
    }

    @JvmStatic
    fun topKeysAt(
        layout: HeatmapCoordinateMap_v1.Snapshot,
        x: Int,
        y: Int,
        maxKeys: Int = 2,
    ): List<ScoredKey> =
        layout.keys
            .map { key -> ScoredKey(key, likelihoodAt(layout, x, y, key)) }
            .filter { it.likelihood > 0.0 }
            .sortedByDescending { it.likelihood }
            .take(maxKeys)

    /** ai-note: returns null when stroke did not pass near key within 25% key-length */
    @JvmStatic
    fun bestLabelAt(
        layout: HeatmapCoordinateMap_v1.Snapshot,
        x: Int,
        y: Int,
    ): String? {
        val top = topKeysAt(layout, x, y, maxKeys = 2)
        if (top.isEmpty()) return null
        if (top.size == 1) return top[0].key.storageLabel
        if (top[0].likelihood < top[1].likelihood * MIN_DOMINANCE_RATIO) return null
        return top[0].key.storageLabel
    }

    @JvmStatic
    fun distanceToBox(x: Int, y: Int, key: HeatmapCoordinateMap_v1.KeyBoundsEntry): Double {
        val dx = when {
            x < key.left -> (key.left - x).toDouble()
            x >= key.right -> (x - key.right + 1).toDouble()
            else -> 0.0
        }
        val dy = when {
            y < key.top -> (key.top - y).toDouble()
            y >= key.bottom -> (y - key.bottom + 1).toDouble()
            else -> 0.0
        }
        return sqrt(dx * dx + dy * dy)
    }
}
