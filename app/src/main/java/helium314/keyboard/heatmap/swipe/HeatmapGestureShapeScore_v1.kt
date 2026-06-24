// SPDX-License-Identifier: GPL-3.0-only

// ai-note: v1 — SHARK2-style geometric score of a candidate word against the gesture.
// Combines a location channel (point-for-point distance), a shape channel (scale/translation
// normalized), an arc-length-ratio prior (separates long sweeps from short words), and
// start/end anchors. Returns a 0..1 geometric probability; the decoder blends word frequency.
package helium314.keyboard.heatmap.swipe

import kotlin.math.exp
import kotlin.math.hypot
import kotlin.math.ln

object HeatmapGestureShapeScore_v1 {

    /** Precomputed, per-gesture features reused across all candidate words. */
    class GestureFeatures(
        val resampled: Array<DoubleArray>,
        val shapeNorm: Array<DoubleArray>,
        val start: DoubleArray,
        val end: DoubleArray,
        val arcLength: Double,
        val keyModel: HeatmapGestureKeyModel_v1,
        val anchors: List<Char>,
    )

    fun features(
        points: List<DoubleArray>,
        keyModel: HeatmapGestureKeyModel_v1,
        timesMs: IntArray = IntArray(0),
    ): GestureFeatures {
        val n = HeatmapGestureTuningConstants_v1.RESAMPLE_N
        val rs = HeatmapGestureResample_v1.resample(points, n)
        val norm = HeatmapGestureResample_v1.shapeNormalize(rs)
        val times = if (timesMs.size == points.size) timesMs else syntheticTimes(points.size)
        val anchors = HeatmapGestureAnchors_v1.extract(points, times, keyModel)
        return GestureFeatures(
            resampled = rs,
            shapeNorm = norm,
            start = points.first().copyOf(),
            end = points.last().copyOf(),
            arcLength = HeatmapGestureResample_v1.arcLength(points),
            keyModel = keyModel,
            anchors = anchors,
        )
    }

    // ai-note: uniform fallback timestamps when callers don't supply real ms (e.g. tests).
    // Disables dwell anchors (all dt are equal), leaving corner + endpoint anchors active.
    private fun syntheticTimes(size: Int): IntArray = IntArray(size) { it * 16 }

    /** Geometric probability (0..1) of [word]; 0.0 if the word can't be placed on the layout. */
    fun score(word: String, g: GestureFeatures): Double {
        val tmpl = HeatmapWordTemplate_v1.build(word, g.keyModel) ?: return 0.0
        val c = HeatmapGestureTuningConstants_v1
        val radius = g.keyModel.radius.coerceAtLeast(1.0)
        val tRs = HeatmapGestureResample_v1.resample(tmpl, c.RESAMPLE_N)

        // Location channel: mean point-to-point distance in key radii.
        var locSum = 0.0
        for (i in tRs.indices) {
            locSum += hypot(g.resampled[i][0] - tRs[i][0], g.resampled[i][1] - tRs[i][1])
        }
        val loc = (locSum / tRs.size) / radius
        val pLoc = exp(-(loc * loc) / (2 * c.SIGMA_LOCATION * c.SIGMA_LOCATION))

        // Shape channel: scale/translation-normalized mean distance.
        val tNorm = HeatmapGestureResample_v1.shapeNormalize(tRs)
        var shapeSum = 0.0
        for (i in tNorm.indices) {
            shapeSum += hypot(g.shapeNorm[i][0] - tNorm[i][0], g.shapeNorm[i][1] - tNorm[i][1])
        }
        val shape = shapeSum / tNorm.size
        val pShape = exp(-(shape * shape) / (2 * c.SIGMA_SHAPE * c.SIGMA_SHAPE))

        val geo = c.W_LOCATION * pLoc + c.W_SHAPE * pShape

        // Arc-length prior: template path length should match the gesture path length.
        val tArc = HeatmapGestureResample_v1.arcLength(tmpl)
        val ratio = (g.arcLength + radius) / (tArc + radius)
        val lr = ln(ratio)
        val arclen = exp(-(lr * lr) / (2 * c.ARCLEN_TAU * c.ARCLEN_TAU))

        // Start (strong) and end (soft) anchors.
        val startD = hypot(tmpl.first()[0] - g.start[0], tmpl.first()[1] - g.start[1]) / radius
        val endD = hypot(tmpl.last()[0] - g.end[0], tmpl.last()[1] - g.end[1]) / radius
        val anchor = exp(-c.ANCHOR_START_W * startD * startD) * exp(-c.ANCHOR_END_W * endD * endD)

        // Corner/dwell coverage: penalize words that miss the gesture's intended-letter anchors.
        // Asymmetric — only missing anchors hurt, so fluid straight passes through a real letter
        // are not punished. This demotes transit-letter intruders (e.g. "nine" for "now").
        val coverage = HeatmapGestureAnchors_v1.coverage(word, g.anchors)
        val coverageFactor = exp(-c.ANCHOR_COVERAGE_K * (1.0 - coverage))

        return geo * arclen * anchor * coverageFactor
    }
}
