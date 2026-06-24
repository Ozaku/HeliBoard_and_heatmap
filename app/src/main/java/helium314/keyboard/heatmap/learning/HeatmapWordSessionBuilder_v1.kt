// SPDX-License-Identifier: GPL-3.0-only

// ai-note: builds WordSession_v4 from decode snapshot for kept + erased attempts

package helium314.keyboard.heatmap.learning

import android.view.inputmethod.EditorInfo
import helium314.keyboard.heatmap.swipe.HeatmapSwipeDecodeSnapshot_v2

object HeatmapWordSessionBuilder_v1 {

    @JvmStatic
    fun fromDecodeSnapshot(
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
        snap: HeatmapSwipeDecodeSnapshot_v2.Snapshot?,
    ): WordSession_v4 = WordSession_v4(
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
        swipeTracePointCount = snap?.strokeTrace?.summary?.pointCount ?: 0,
        runnerUpWords = snap?.runnerUpWords ?: emptyList(),
        swipeInferredPath = snap?.pathLettersDeduped?.joinToString(""),
        swipeInferredPathRaw = snap?.pathLettersRaw?.joinToString(""),
        swipeIntentPath = snap?.intentPathLetters?.joinToString(""),
        swipeVisitOrder = snap?.visitOrder?.joinToString(""),
        swipeStartDistribution = snap?.startDistribution ?: emptyList(),
        swipeDwellSegments = snap?.diagnostics?.dwellSegments ?: emptyList(),
        swipeTracePoints = snap?.strokeTrace?.points ?: emptyList(),
        swipeDecodeDiagnostics = snap?.diagnostics,
        swipeTargetWord = snap?.targetWord,
        swipeStyle = snap?.swipeStyle,
        swipeOutcomeCorrect = snap?.targetWord?.let { target ->
            text.equals(target, ignoreCase = true)
        },
    )

    @JvmStatic
    fun tapAttempt(
        slotId: WordSlotId_v1,
        sessionGeneration: Long,
        hostPackage: String?,
        text: String,
        separatorCharCount: Int,
        outcome: WordSessionOutcome_v1,
        memorySequence: Int,
        linkedSlotId: WordSlotId_v1?,
    ): WordSession_v4 = WordSession_v4(
        slotId = slotId,
        sessionGeneration = sessionGeneration,
        hostPackage = hostPackage,
        inputMode = WordSessionInputMode_v1.TAP,
        commitType = WordSessionCommitType_v1.USER_TYPED,
        committedText = text,
        finalText = text,
        typedText = text,
        separatorCharCount = separatorCharCount,
        committedAtMs = System.currentTimeMillis(),
        wordMemoryOutcome = outcome,
        memorySequence = memorySequence,
        linkedSlotId = linkedSlotId,
    )
}
