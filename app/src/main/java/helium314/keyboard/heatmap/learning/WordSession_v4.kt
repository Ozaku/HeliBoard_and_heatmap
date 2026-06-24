// SPDX-License-Identifier: GPL-3.0-only

// ai-note: v4 adds wordMemoryOutcome + sequence for full attempt history (kept + erased)

package helium314.keyboard.heatmap.learning

import helium314.keyboard.heatmap.swipe.HeatmapKeyLikelihood_v6
import helium314.keyboard.heatmap.swipe.HeatmapSwipeDecodeDiagnostics_v1
import helium314.keyboard.heatmap.swipe.HeatmapSwipeStrokeKinematics_v1
import helium314.keyboard.heatmap.swipe.HeatmapSwipeStrokeTrace_v1

data class WordSession_v4(

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

    /** ai-note: monotonic per IME session — orders swipe/tap attempts including erasures */
    val memorySequence: Int = 0,

    /** ai-note: when erased, links to slot of related kept attempt if any */
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

    }

    /** ai-note: char budget weight for 300-word / 3000-char memory window */
    fun memoryCharWeight(): Int =
        committedText.length + separatorCharCount.coerceAtLeast(0) + 1
}
