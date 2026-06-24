// SPDX-License-Identifier: GPL-3.0-only

// ai-note: Phase1 step 1.2 — trace + diagnostics + calibration fields for commit export

package helium314.keyboard.heatmap.swipe

object HeatmapSwipeDecodeSnapshot_v2 {

    private const val MAX_RUNNER_UPS = 8

    data class Snapshot(
        val pathLettersRaw: List<String>,
        val pathLettersDeduped: List<String>,
        val intentPathLetters: List<String>,
        val visitOrder: List<String>,
        val startDistribution: List<HeatmapKeyLikelihood_v6.LabelWeight>,
        val runnerUpWords: List<String>,
        val strokeTrace: HeatmapSwipeStrokeTrace_v1.Bundle?,
        val diagnostics: HeatmapSwipeDecodeDiagnostics_v1.Bundle?,
        val targetWord: String?,
        val swipeStyle: String?,
    )

    private val holder = ThreadLocal<Snapshot?>()

    @JvmStatic
    fun stash(
        infer: HeatmapSwipeSegmentInfer_v19.Result,
        rankedWords: List<String>,
        diagnostics: HeatmapSwipeDecodeDiagnostics_v1.Bundle?,
    ) {
        holder.set(
            Snapshot(
                pathLettersRaw = infer.pathLettersRaw,
                pathLettersDeduped = infer.pathLetters,
                intentPathLetters = infer.intentPathLetters,
                visitOrder = infer.strokeOrderLetters,
                startDistribution = infer.startDistribution,
                runnerUpWords = rankedWords.filter { it.isNotEmpty() }.take(MAX_RUNNER_UPS),
                strokeTrace = infer.strokeTrace,
                diagnostics = diagnostics,
                targetWord = null,
                swipeStyle = null,
            ),
        )
    }

    @JvmStatic
    fun consume(): Snapshot? {
        val snap = holder.get()
        holder.set(null)
        return snap
    }

    @JvmStatic
    fun peek(): Snapshot? = holder.get()
}
