// SPDX-License-Identifier: GPL-3.0-only

// ai-note: v3 — cross-thread global snapshot (decode thread stashes, main thread consumes)

package helium314.keyboard.heatmap.swipe

import java.util.concurrent.atomic.AtomicReference

object HeatmapSwipeDecodeSnapshot_v3 {

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

    private val holder = AtomicReference<Snapshot?>(null)

    @JvmStatic
    fun stash(
        infer: HeatmapSwipeSegmentInfer_v19.Result,
        rankedWords: List<String>,
        diagnostics: HeatmapSwipeDecodeDiagnostics_v1.Bundle?,
    ) {
        val top = rankedWords.firstOrNull { it.isNotEmpty() }
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
                targetWord = top,
                swipeStyle = if (infer.kinematics.isSlowStroke) "slow" else "fast",
            ),
        )
    }

    @JvmStatic
    fun consume(): Snapshot? = holder.getAndSet(null)

    @JvmStatic
    fun peek(): Snapshot? = holder.get()
}
