// SPDX-License-Identifier: GPL-3.0-only

// ai-note: tuning revision 2 — long-word visit path expansion (see IntentPath_v2)

package helium314.keyboard.heatmap.swipe

object HeatmapSwipeTuningConstants_v2 {

    /** ai-note: increment when defaults change from baseline calibration pass */
    const val TUNING_REVISION = 2

    const val DWELL_MIN_MS = HeatmapSwipeTuningConstants_v1.DWELL_MIN_MS
    const val DWELL_SPEED_KEYWIDTHS_PER_SEC = HeatmapSwipeTuningConstants_v1.DWELL_SPEED_KEYWIDTHS_PER_SEC
    const val TRANSIT_SPEED_KEYWIDTHS_PER_SEC = HeatmapSwipeTuningConstants_v1.TRANSIT_SPEED_KEYWIDTHS_PER_SEC
    const val SLOW_STROKE_AVG_KEYWIDTHS_PER_SEC = HeatmapSwipeTuningConstants_v1.SLOW_STROKE_AVG_KEYWIDTHS_PER_SEC
    const val CORNER_ANGLE_DEG = HeatmapSwipeTuningConstants_v1.CORNER_ANGLE_DEG
    const val CORNER_DECEL_REQUIRED = HeatmapSwipeTuningConstants_v1.CORNER_DECEL_REQUIRED
    const val MAX_DICT_PATH_SKIPS = HeatmapSwipeTuningConstants_v1.MAX_DICT_PATH_SKIPS
    const val START_NEIGHBOR_HOP_RADIUS = HeatmapSwipeTuningConstants_v1.START_NEIGHBOR_HOP_RADIUS
    const val START_HARD_REJECT_HOPS = HeatmapSwipeTuningConstants_v1.START_HARD_REJECT_HOPS
    const val DWELL_DOUBLE_MIN_MS = HeatmapSwipeTuningConstants_v1.DWELL_DOUBLE_MIN_MS
    const val EXPORT_TRACE_POINT_CAP = HeatmapSwipeTuningConstants_v1.EXPORT_TRACE_POINT_CAP
    const val SWIPE_RING_BUFFER_SIZE = HeatmapSwipeTuningConstants_v1.SWIPE_RING_BUFFER_SIZE
}

