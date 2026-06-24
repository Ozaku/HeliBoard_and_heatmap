// SPDX-License-Identifier: GPL-3.0-only

// ai-note: v2 adds host app package (EditorInfo) for cross-app session debugging

package helium314.keyboard.heatmap.learning



data class WordSession_v2(

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

    /** ai-note: deduped inferred path from last swipe decode (export/debug). */
    val swipeInferredPath: String? = null,

    /** ai-note: raw beat labels before consecutive-key collapse. */
    val swipeInferredPathRaw: String? = null,

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

