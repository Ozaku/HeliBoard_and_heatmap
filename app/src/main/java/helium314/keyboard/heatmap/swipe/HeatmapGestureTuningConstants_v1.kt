// SPDX-License-Identifier: GPL-3.0-only

// ai-note: v1 — SHARK2-style gesture-template decoder tuning constants.
// Values validated offline against pulled 0.0.0.52 traces (data_pull/shark2_harness_v1.py):
// recovered cyclical / system / into as the top candidate where the old corner-extractor
// produced CLI / SDS / I. Revision 5 marked the architecture switch (old corner-extraction
// pipeline stamped revisions 1..4); revision 6 adds the corner/dwell anchor-coverage accuracy
// pass (demotes transit-letter intruders like "nine" for "now"). Persisted diagnostics read
// this revision so the export no longer mismatches the live decoder.
package helium314.keyboard.heatmap.swipe

object HeatmapGestureTuningConstants_v1 {

    const val TUNING_REVISION = 6

    // Points each polyline (gesture + word template) is resampled to before comparison.
    const val RESAMPLE_N = 48

    // Channel weights for the geometric probability.
    const val W_LOCATION = 0.70
    const val W_SHAPE = 0.30

    // Gaussian widths (in key radii / normalized units) converting distance to probability.
    const val SIGMA_LOCATION = 0.85
    const val SIGMA_SHAPE = 0.55

    // Start anchor is strong (first letter is a hard-ish anchor per LOCKED design 48_),
    // end anchor is softer.
    const val ANCHOR_START_W = 0.60
    const val ANCHOR_END_W = 0.40

    // Arc-length log-ratio prior width. This is the signal that separates a long multi-corner
    // sweep (e.g. "cyclical") from a short common word ("cup") that is also a subsequence.
    const val ARCLEN_TAU = 0.45

    // Frequency prior exponent. Kept weak so geometry dominates and rare-but-correct words win.
    const val FREQ_ALPHA = 0.15

    // Neighbor tolerance as a multiple of inter-key spacing (used for candidate pruning).
    const val NEIGHBOR_FACTOR = 1.45

    // Candidate-pool / output caps.
    const val MAX_CANDIDATES = 4000
    const val MAX_WORD_LEN = 24
    const val MAX_RESULTS_FULL = 32
    const val MAX_RESULTS_PREVIEW = 20

    // Skip building the lexicon for words rarer than this raw probability (0 keeps all).
    const val MIN_WORD_FREQUENCY = 0

    // --- Corner/dwell anchor coverage (v1 accuracy pass, validated on 0.0.0.53 traces) ---
    // Intended letters sit at sharp corners, sustained pauses, or the stroke endpoints; letters
    // swept over fast and straight (classic 'i' on the top row) are transit. We extract those
    // strong anchors and penalize candidate words that FAIL to cover them in order. We only
    // penalize missing anchors (asymmetric) — a correct word may legitimately pass straight
    // through a middle letter, so absence of a corner there is NOT held against it.
    // Sweep over real device runner-up lists: this preserved top-1 (30/47, == base) while
    // demoting 26/35 transit-letter intruders below rank 3.
    //
    // Minimum turning angle (radians) for a path vertex to count as an intended corner (~63deg).
    const val ANCHOR_CORNER_MIN_RAD = 1.10
    // Minimum dwell time (ms) the finger must linger near one spot to count as an intended pause.
    const val ANCHOR_DWELL_MIN_MS = 90
    // Radius (in key radii) within which consecutive samples count as the "same spot" for dwell.
    const val ANCHOR_DWELL_RADIUS_FRAC = 0.65
    // Min segment length (key radii) used when measuring the turn angle, to reject touch jitter.
    const val ANCHOR_WINDOW_MIN_DIST_FRAC = 0.40
    // Penalty strength for missing anchors: score *= exp(-K * (1 - coverage)).
    const val ANCHOR_COVERAGE_K = 1.0
}
