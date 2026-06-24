// SPDX-License-Identifier: GPL-3.0-only

// ai-note: v4 — builds WordSession_v5 (with v6 precision fields) from the v3 decode snapshot, using
// the anchor-based HeatmapSwipeGeometryVector_v2 (validated detector) instead of the legacy v1 one,
// and stamping position-aware fields: fieldStartOffset/fieldEndOffset/coherence.
// Mirrors HeatmapWordSessionBuilder_v3 otherwise; used exclusively by HeatmapWordSlotSession_v7.
package helium314.keyboard.heatmap.learning

import android.content.Context
import helium314.keyboard.heatmap.swipe.HeatmapSwipeDecodeSnapshot_v3
import helium314.keyboard.heatmap.swipe.HeatmapSwipeTuningConstants_v3

object HeatmapWordSessionBuilder_v4 {

    @JvmStatic
    fun fromDecodeSnapshot(
        context: Context,
        slotId: WordSlotId_v1,
        sessionGeneration: Long,
        hostPackage: String?,
        isGestureInput: Boolean,
        latinCommitType: Int,
        text: String,
        typedText: String?,
        separatorCharCount: Int,
        outcome: WordSessionOutcome_v1,
        memorySequence: Int,
        linkedSlotId: WordSlotId_v1?,
        snap: HeatmapSwipeDecodeSnapshot_v3.Snapshot?,
        fieldStartOffset: Int,
        fieldEndOffset: Int,
        coherence: WordSessionCoherence_v1,
    ): WordSession_v5 {
        val geometry = buildSwipeGeometry(context, isGestureInput, text)
        val tracePoints = capTracePoints(snap?.strokeTrace?.points ?: emptyList())
        return WordSession_v5(
            slotId = slotId,
            sessionGeneration = sessionGeneration,
            hostPackage = hostPackage,
            inputMode = WordSessionInputMode_v1.fromGestureInput(isGestureInput),
            commitType = WordSessionCommitType_v1.fromLatinCommitType(latinCommitType),
            committedText = text,
            finalText = text,
            typedText = typedText ?: text,
            separatorCharCount = separatorCharCount,
            committedAtMs = System.currentTimeMillis(),
            wordMemoryOutcome = outcome,
            memorySequence = memorySequence,
            linkedSlotId = linkedSlotId,
            swipeTracePointCount = snap?.strokeTrace?.summary?.pointCount
                ?: geometry?.pointCount
                ?: 0,
            runnerUpWords = snap?.runnerUpWords ?: emptyList(),
            swipeInferredPath = snap?.pathLettersDeduped?.joinToString(""),
            swipeInferredPathRaw = snap?.pathLettersRaw?.joinToString(""),
            swipeIntentPath = snap?.intentPathLetters?.joinToString(""),
            swipeVisitOrder = snap?.visitOrder?.joinToString(""),
            swipeStartDistribution = snap?.startDistribution ?: emptyList(),
            swipeDwellSegments = snap?.diagnostics?.dwellSegments ?: emptyList(),
            swipeTracePoints = tracePoints,
            swipeDecodeDiagnostics = snap?.diagnostics,
            swipeTargetWord = snap?.targetWord,
            swipeStyle = snap?.swipeStyle,
            swipeOutcomeCorrect = snap?.targetWord?.let { target ->
                text.equals(target, ignoreCase = true)
            },
            swipeGeometry = geometry,
            fieldStartOffset = fieldStartOffset,
            fieldEndOffset = fieldEndOffset,
            coherence = coherence,
        )
    }

    private fun capTracePoints(
        points: List<helium314.keyboard.heatmap.swipe.HeatmapSwipeStrokeTrace_v1.TracePoint>,
    ): List<helium314.keyboard.heatmap.swipe.HeatmapSwipeStrokeTrace_v1.TracePoint> {
        val cap = HeatmapSwipeTuningConstants_v3.EXPORT_TRACE_POINT_CAP
        if (points.size <= cap) return points
        val step = points.size.toDouble() / cap
        return (0 until cap).map { i ->
            points[(i * step).toInt().coerceIn(0, points.lastIndex)]
        }
    }

    private fun buildSwipeGeometry(
        context: Context,
        isGestureInput: Boolean,
        finalWord: String,
    ): HeatmapSwipeGeometryVector_v1.Vector? {
        if (!isGestureInput) return null
        val stash = HeatmapPathCapture_v3.peek() ?: return null
        if (stash.pointers.pointerSize < 2) return null
        val layout = HeatmapLayoutContext_v2.captureForCommit(context) ?: return null
        return HeatmapSwipeGeometryVector_v3.build(stash.pointers, layout, finalWord)
    }
}
