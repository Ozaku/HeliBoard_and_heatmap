// SPDX-License-Identifier: GPL-3.0-only

// ai-note: Phase1 step 1.2 — per-decode diagnostics for export and threshold tuning

package helium314.keyboard.heatmap.swipe

object HeatmapSwipeDecodeDiagnostics_v1 {

    data class CandidateDiag(val word: String, val score: Double)

    data class Bundle(
        val tuningRevision: Int,
        val pathLen: Int,
        val strokeLen: Int,
        val intentPath: String,
        val visitOrder: String,
        val transitKeyCount: Int,
        val dwellSegments: List<HeatmapSwipeStrokeKinematics_v1.DwellSegment>,
        val prefixesTried: List<String>,
        val topCandidates: List<CandidateDiag>,
        val avgSpeedKeyWidthsPerSec: Double,
        val isSlowStroke: Boolean,
        val decodePointCount: Int,
        val stashPointCount: Int,
        val pointerIntegrityOk: Boolean,
    )

    @JvmStatic
    fun build(
        infer: HeatmapSwipeSegmentInfer_v19.Result,
        kinematics: HeatmapSwipeStrokeKinematics_v1.Result,
        intent: HeatmapSwipeIntentClassifier_v1.Result,
        prefixes: List<String>,
        scored: List<Pair<helium314.keyboard.latin.SuggestedWords.SuggestedWordInfo, Double>>,
        decodePointCount: Int,
        stashPointCount: Int,
    ): Bundle = Bundle(
        tuningRevision = HeatmapSwipeTuningConstants_v1.TUNING_REVISION,
        pathLen = infer.pathLetters.size,
        strokeLen = infer.strokeOrderLetters.size,
        intentPath = infer.intentPathLetters.joinToString(""),
        visitOrder = intent.visitOrder.joinToString(""),
        transitKeyCount = intent.transitKeys.size,
        dwellSegments = kinematics.dwellSegments,
        prefixesTried = prefixes.take(12),
        topCandidates = scored.take(5).map { (info, score) ->
            CandidateDiag(info.mWord?.toString().orEmpty(), score)
        },
        avgSpeedKeyWidthsPerSec = kinematics.avgSpeedKeyWidthsPerSec,
        isSlowStroke = kinematics.isSlowStroke,
        decodePointCount = decodePointCount,
        stashPointCount = stashPointCount,
        pointerIntegrityOk = stashPointCount <= 0 || decodePointCount == stashPointCount,
    )
}
