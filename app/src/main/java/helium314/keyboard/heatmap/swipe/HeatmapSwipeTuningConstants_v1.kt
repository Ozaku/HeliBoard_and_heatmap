// SPDX-License-Identifier: GPL-3.0-only

// ai-note: Phase1 step 1.5 — central swipe thresholds; bump tuningRevision after device calibration

package helium314.keyboard.heatmap.swipe

object HeatmapSwipeTuningConstants_v1 {

    /** ai-note: increment when defaults change from baseline calibration pass */
    const val TUNING_REVISION = 1

    /** ai-note: dwell — user slowed on key long enough to intend letter */
    const val DWELL_MIN_MS = 450L
    const val DWELL_SPEED_KEYWIDTHS_PER_SEC = 2.2

    /** ai-note: transit — fast pass over key without intending it (purposes over U) */
    const val TRANSIT_SPEED_KEYWIDTHS_PER_SEC = 4.5

    /** ai-note: slow stroke avg speed — enables visit-order fallback for intent path */
    const val SLOW_STROKE_AVG_KEYWIDTHS_PER_SEC = 2.8

    /** ai-note: corner beat detection */
    const val CORNER_ANGLE_DEG = 16.0
    const val CORNER_DECEL_REQUIRED = true

    /** ai-note: dict path alignment — skipped letters in fast swipe */
    const val MAX_DICT_PATH_SKIPS = 2

    /** ai-note: soft start — neighbor hop radius on QWERTY graph */
    const val START_NEIGHBOR_HOP_RADIUS = 1
    const val START_HARD_REJECT_HOPS = 3

    /** ai-note: double letter hint from dwell duration */
    const val DWELL_DOUBLE_MIN_MS = 520L

    /** ai-note: export trace cap per swipe — raised 128->512 for full-fidelity raw path capture
     *  (anchor-driven decoder tuning needs the complete x,y,t stream, not a downsample). All
     *  v2/v3/v4 swipe-tuning aliases chain to this value, so both the trace ring and the session
     *  builder export up to 512 samples. Real Android gestures rarely exceed ~60 samples, so this
     *  is headroom that only truncates pathological inputs. */
    const val EXPORT_TRACE_POINT_CAP = 512

    /** ai-note: rolling ring size in session export root */
    const val SWIPE_RING_BUFFER_SIZE = 20
}
