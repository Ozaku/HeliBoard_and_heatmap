// SPDX-License-Identifier: GPL-3.0-only

// ai-note: v2 (TUNING_REVISION 7) — anchor-DRIVEN decoder tuning. v1 (rev 6) used the transit
// crossing sequence to generate candidates and only used anchors as a weak score multiplier, which
// let words be built from keys the finger merely passed over (eras/piura/flying). v2 makes the
// gesture's intended-letter ANCHORS (start + hard stops + corners + end) the primary driver:
//   - velocity-minima STOP detection so brief hard stops always register as letters
//   - STRICT length binding: a word's collapsed-run letters must equal the anchor count
//     (doubled letters allowed, each anchor doublable once)
// Geometry params mirror v1 (validated on 0.0.0.52/0.0.0.53 traces); only the candidate-generation
// model changed. Persisted diagnostics read TUNING_REVISION so exports match the live decoder.
package helium314.keyboard.heatmap.swipe

object HeatmapGestureTuningConstants_v2 {

    const val TUNING_REVISION = 8

    // --- Geometry channels (mirror v1) ---
    const val RESAMPLE_N = 48
    const val W_LOCATION = 0.70
    const val W_SHAPE = 0.30
    const val SIGMA_LOCATION = 0.85
    const val SIGMA_SHAPE = 0.55
    const val ANCHOR_START_W = 0.60
    const val ANCHOR_END_W = 0.40
    const val ARCLEN_TAU = 0.45
    const val FREQ_ALPHA = 0.15
    const val NEIGHBOR_FACTOR = 1.45

    // --- Output / pool caps ---
    const val MAX_WORD_LEN = 24
    const val MAX_RESULTS_FULL = 32
    const val MAX_RESULTS_PREVIEW = 20
    const val MIN_WORD_FREQUENCY = 0
    // Anchor-aligned pools are tiny; cap generously for the relaxed fallback passes.
    const val ANCHOR_MAX_CANDIDATES = 2000
    // Transit-subsequence last-resort fallback cap (legacy behavior, ranked below strict).
    const val FALLBACK_MAX_CANDIDATES = 4000

    // --- Corner detection (mirror v1 anchors) ---
    // Minimum turning angle (radians) for a path vertex to count as an intended corner (~63deg).
    const val ANCHOR_CORNER_MIN_RAD = 1.10
    // Min segment length (key radii) used when measuring the turn angle, to reject touch jitter.
    const val ANCHOR_WINDOW_MIN_DIST_FRAC = 0.40

    // --- Dwell (time-near-a-spot) detection (mirror v1 anchors) ---
    const val ANCHOR_DWELL_MIN_MS = 90
    const val ANCHOR_DWELL_RADIUS_FRAC = 0.65

    // --- Velocity-minima STOP detection (NEW — the key fix for "a hard stop is always a letter") ---
    // A point is a candidate stop when its smoothed speed dips below this fraction of the gesture's
    // mean speed AND is a local minimum. Catches brief stops the time-based dwell detector missed.
    const val STOP_SPEED_FRAC = 0.45
    // Two stops nearer than this (in key radii) collapse into one anchor (avoids double-counting).
    const val STOP_MIN_SEPARATION_FRAC = 0.55
    // Smoothing half-window (samples) for the per-point speed used in minima detection.
    // Increased to 4 (9-point window) to smooth over Android's batched touch events (0 dt spikes).
    const val STOP_SPEED_SMOOTH_HALF = 4

    // --- Corner clustering ---
    // Two corners nearer than this (in key radii) collapse into one anchor (avoids double-counting
    // on slow, rounded physical turns).
    const val CORNER_CLUSTER_MAX_SEPARATION_FRAC = 0.75

    // --- Strict alignment / doubles / relax ---
    // Max times a single anchor letter may be doubled in the word (cheese: e doubled once).
    const val MAX_DOUBLES_PER_ANCHOR = 1
    // Graded fallback when the strict pool is empty: allow this many missing/extra anchors.
    const val ANCHOR_RELAX_SLACK = 1
    // Geometric-score penalty applied to fallback-tier candidates so they always rank below
    // strict matches (they only surface when the strict pool was empty).
    const val FALLBACK_GEO_PENALTY = 0.60

    // --- Export trace fidelity (full raw path; was 128) ---
    const val EXPORT_TRACE_POINT_CAP = 512
}
