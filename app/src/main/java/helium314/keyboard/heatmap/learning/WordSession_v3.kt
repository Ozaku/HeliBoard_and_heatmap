// SPDX-License-Identifier: GPL-3.0-only

// ai-note: Phase1 step 1.3 — WordSession with swipe trace + calibration export fields

package helium314.keyboard.heatmap.learning

import helium314.keyboard.heatmap.swipe.HeatmapKeyLikelihood_v6
import helium314.keyboard.heatmap.swipe.HeatmapSwipeDecodeDiagnostics_v1
import helium314.keyboard.heatmap.swipe.HeatmapSwipeStrokeKinematics_v1
import helium314.keyboard.heatmap.swipe.HeatmapSwipeStrokeTrace_v1

data class WordSession_v3(

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

    val swipeTracePointCount: Int = 0,

    val tapKeyCount: Int = 0,

    val runnerUpWords: List<String> = emptyList(),

    val swipeInferredPath: String? = null,

    val swipeInferredPathRaw: String? = null,

    /** ai-note: intent path from dwell/corners (Phase1) */
    val swipeIntentPath: String? = null,

    val swipeVisitOrder: String? = null,

    val swipeStartDistribution: List<HeatmapKeyLikelihood_v6.LabelWeight> = emptyList(),

    val swipeDwellSegments: List<HeatmapSwipeStrokeKinematics_v1.DwellSegment> = emptyList(),

    val swipeTracePoints: List<HeatmapSwipeStrokeTrace_v1.TracePoint> = emptyList(),

    val swipeDecodeDiagnostics: HeatmapSwipeDecodeDiagnostics_v1.Bundle? = null,

    /** ai-note: optional test matrix label for baseline calibration ingest */
    val swipeTargetWord: String? = null,

    val swipeStyle: String? = null,

    val swipeOutcomeCorrect: Boolean? = null,

) {

    fun debugOneLine(): String = buildString {

        append("WordSlot#").append(slotId.value)

        hostPackage?.let { append(" host=").append(it) }

        append(' ').append(inputMode.name.lowercase())

        append(' ').append(commitType.name.lowercase())

        append(" committed=").append(committedText)

        if (typedText != committedText) append(" typed=").append(typedText)

    }
}
