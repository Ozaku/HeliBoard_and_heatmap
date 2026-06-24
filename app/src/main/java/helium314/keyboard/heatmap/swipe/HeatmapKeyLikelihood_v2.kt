// SPDX-License-Identifier: GPL-3.0-only

// ai-note: Block 3 step 15h — tighter key hit radius; keys a full row away (F→A) get 0 likelihood

package helium314.keyboard.heatmap.swipe

import helium314.keyboard.heatmap.learning.HeatmapCoordinateMap_v1
import kotlin.math.sqrt

object HeatmapKeyLikelihood_v2 {

    data class ScoredKey(
        val key: HeatmapCoordinateMap_v1.KeyBoundsEntry,
        val likelihood: Double,
    )

    /** ai-note: was 1.0 in v1 — too loose; A could win when stroke grazed E */
    private const val DIST_ZERO_FACTOR = 0.58

    private const val MIN_BEST_LIKELIHOOD = 0.34

    @JvmStatic
    fun likelihoodAt(
        layout: HeatmapCoordinateMap_v1.Snapshot,
        x: Int,
        y: Int,
        key: HeatmapCoordinateMap_v1.KeyBoundsEntry,
    ): Double {
        val dist = distanceToBox(x, y, key)
        if (dist <= 0.0) return 1.0
        val keyWidth = (key.right - key.left).coerceAtLeast(1)
        val keyHeight = (key.bottom - key.top).coerceAtLeast(1)
        val zeroThresh = maxOf(keyWidth, keyHeight) * DIST_ZERO_FACTOR
        if (dist >= zeroThresh) return 0.0
        return 1.0 - (dist / zeroThresh)
    }

    @JvmStatic
    fun topKeysAt(
        layout: HeatmapCoordinateMap_v1.Snapshot,
        x: Int,
        y: Int,
        maxKeys: Int = 2,
        minLikelihood: Double = MIN_BEST_LIKELIHOOD,
    ): List<ScoredKey> =
        layout.keys
            .map { key -> ScoredKey(key, likelihoodAt(layout, x, y, key)) }
            .filter { it.likelihood >= minLikelihood }
            .sortedByDescending { it.likelihood }
            .take(maxKeys)

    @JvmStatic
    fun bestLabelAt(
        layout: HeatmapCoordinateMap_v1.Snapshot,
        x: Int,
        y: Int,
    ): String? = topKeysAt(layout, x, y, maxKeys = 1).firstOrNull()?.key?.storageLabel

    private fun distanceToBox(x: Int, y: Int, key: HeatmapCoordinateMap_v1.KeyBoundsEntry): Double {
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
