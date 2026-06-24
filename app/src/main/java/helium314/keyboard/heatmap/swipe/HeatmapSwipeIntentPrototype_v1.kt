// SPDX-License-Identifier: GPL-3.0-only

// ai-note: Phase1 step 5.2 — read-only OOTB defaults; refreshed offline from test corpus calibration

package helium314.keyboard.heatmap.swipe

object HeatmapSwipeIntentPrototype_v1 {

    /** ai-note: which calibration revision baked these defaults */
    val tuningRevision: Int get() = HeatmapSwipeTuningConstants_v1.TUNING_REVISION

    val dwellMinMs: Long get() = HeatmapSwipeTuningConstants_v1.DWELL_MIN_MS
    val dwellSpeedKeyWidthsPerSec: Double get() = HeatmapSwipeTuningConstants_v1.DWELL_SPEED_KEYWIDTHS_PER_SEC
    val transitSpeedKeyWidthsPerSec: Double get() = HeatmapSwipeTuningConstants_v1.TRANSIT_SPEED_KEYWIDTHS_PER_SEC
    val slowStrokeAvgKeyWidthsPerSec: Double get() = HeatmapSwipeTuningConstants_v1.SLOW_STROKE_AVG_KEYWIDTHS_PER_SEC
    val cornerAngleDeg: Double get() = HeatmapSwipeTuningConstants_v1.CORNER_ANGLE_DEG
    val maxDictPathSkips: Int get() = HeatmapSwipeTuningConstants_v1.MAX_DICT_PATH_SKIPS
    val startNeighborHopRadius: Int get() = HeatmapSwipeTuningConstants_v1.START_NEIGHBOR_HOP_RADIUS
    val startHardRejectHops: Int get() = HeatmapSwipeTuningConstants_v1.START_HARD_REJECT_HOPS

    /** ai-note: position weight multipliers for scoring (first/middle/last) */
    const val WEIGHT_FIRST = 1.35
    const val WEIGHT_MIDDLE = 1.0
    const val WEIGHT_LAST = 1.25
}
