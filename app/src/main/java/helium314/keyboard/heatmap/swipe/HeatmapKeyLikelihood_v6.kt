// SPDX-License-Identifier: GPL-3.0-only

// ai-note: Phase1 step 2.2 — soft label distributions; v5 strict bestLabelAt retained for gates

package helium314.keyboard.heatmap.swipe

import helium314.keyboard.heatmap.learning.HeatmapCoordinateMap_v1

object HeatmapKeyLikelihood_v6 {

    data class LabelWeight(val label: String, val likelihood: Double)

    private const val MIN_DOMINANCE_RATIO = 2.5

    @JvmStatic
    fun distributionAt(
        layout: HeatmapCoordinateMap_v1.Snapshot,
        x: Int,
        y: Int,
        maxKeys: Int = 3,
    ): List<LabelWeight> {
        val top = HeatmapKeyLikelihood_v5.topKeysAt(layout, x, y, maxKeys = maxKeys)
        if (top.isEmpty()) return emptyList()
        val sum = top.sumOf { it.likelihood }.coerceAtLeast(1e-9)
        return top.map { LabelWeight(it.key.storageLabel, it.likelihood / sum) }
    }

    /** ai-note: strict single label when clear; else top label from distribution */
    @JvmStatic
    fun bestLabelAt(
        layout: HeatmapCoordinateMap_v1.Snapshot,
        x: Int,
        y: Int,
    ): String? {
        val strict = HeatmapKeyLikelihood_v5.bestLabelAt(layout, x, y)
        if (strict != null) return strict
        return distributionAt(layout, x, y, maxKeys = 2).firstOrNull()?.label
    }

    @JvmStatic
    fun primaryStartLabel(distribution: List<LabelWeight>): String? =
        distribution.maxByOrNull { it.likelihood }?.label

    /** ai-note: visit-order path — always pick top key; never null-gap on ambiguous touches */
    @JvmStatic
    fun bestLabelForPath(
        layout: HeatmapCoordinateMap_v1.Snapshot,
        x: Int,
        y: Int,
    ): String? = distributionAt(layout, x, y, maxKeys = 1).firstOrNull()?.label
        ?: HeatmapKeyLikelihood_v5.bestLabelAt(layout, x, y)
}
