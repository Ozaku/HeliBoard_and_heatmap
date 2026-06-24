// SPDX-License-Identifier: GPL-3.0-only

// ai-note: v5 adds swipeGeometry + correction-chain metadata on top of v4 word memory

package helium314.keyboard.heatmap.learning

import helium314.keyboard.heatmap.swipe.HeatmapKeyLikelihood_v6
import helium314.keyboard.heatmap.swipe.HeatmapSwipeDecodeDiagnostics_v1
import helium314.keyboard.heatmap.swipe.HeatmapSwipeStrokeKinematics_v1
import helium314.keyboard.heatmap.swipe.HeatmapSwipeStrokeTrace_v1

data class WordSession_v5(

    val slotId: WordSlotId_v1,

    val sessionGeneration: Long,

    val hostPackage: String?,

    val inputMode: WordSessionInputMode_v1,

    val commitType: WordSessionCommitType_v1,

    val committedText: String,

    val finalText: String,

    val typedText: String,

    val separatorCharCount: Int,

    val committedAtMs: Long,

    val wordMemoryOutcome: WordSessionOutcome_v1 = WordSessionOutcome_v1.KEPT_IN_FIELD,

    val memorySequence: Int = 0,

    val linkedSlotId: WordSlotId_v1? = null,

    val swipeTracePointCount: Int = 0,

    val tapKeyCount: Int = 0,

    val runnerUpWords: List<String> = emptyList(),

    val swipeInferredPath: String? = null,

    val swipeInferredPathRaw: String? = null,

    val swipeIntentPath: String? = null,

    val swipeVisitOrder: String? = null,

    val swipeStartDistribution: List<HeatmapKeyLikelihood_v6.LabelWeight> = emptyList(),

    val swipeDwellSegments: List<HeatmapSwipeStrokeKinematics_v1.DwellSegment> = emptyList(),

    val swipeTracePoints: List<HeatmapSwipeStrokeTrace_v1.TracePoint> = emptyList(),

    val swipeDecodeDiagnostics: HeatmapSwipeDecodeDiagnostics_v1.Bundle? = null,

    val swipeTargetWord: String? = null,

    val swipeStyle: String? = null,

    val swipeOutcomeCorrect: Boolean? = null,

    val swipeGeometry: HeatmapSwipeGeometryVector_v1.Vector? = null,

    val correctionChainId: Int? = null,

    val correctionAttemptIndex: Int? = null,

    val correctionFinalWord: String? = null,

    // ai-note: v6/position-aware precision fields (additive; default = unknown / clean).
    /** absolute char offset where this word's composing region began (-1 if unknown) */
    val fieldStartOffset: Int = -1,

    /** absolute char offset just past this word in the field (-1 if unknown) */
    val fieldEndOffset: Int = -1,

    /** trust level of this session's position/attribution (drives offline filtering) */
    val coherence: WordSessionCoherence_v1 = WordSessionCoherence_v1.CLEAN,

    /** slot that later overwrote/replaced this position, if any */
    val supersededBySlotId: WordSlotId_v1? = null,

    /** user likely abandoned/changed their mind about this word (trailing-delete-to-fix-earlier etc.) */
    val possibleMindChange: Boolean = false,

    /** edit happened via a cursor jump far from recent typing (proofreading) */
    val proofreadEdit: Boolean = false,

) {

    fun debugOneLine(): String = buildString {

        append("WordSlot#").append(slotId.value)

        append(" mem#").append(memorySequence)

        append(' ').append(wordMemoryOutcome.name.lowercase())

        hostPackage?.let { append(" host=").append(it) }

        append(' ').append(inputMode.name.lowercase())

        append(' ').append(commitType.name.lowercase())

        append(" text=").append(committedText)

        if (typedText != committedText) append(" typed=").append(typedText)

        correctionChainId?.let { append(" chain=").append(it) }

    }

    fun memoryCharWeight(): Int =
        committedText.length + separatorCharCount.coerceAtLeast(0) + 1
}
