// SPDX-License-Identifier: GPL-3.0-only

// ai-note: per-word session record — step 1.8 schema; stroke lists stubbed until later substeps

package helium314.keyboard.heatmap.learning



/**

 * One committed word in the paragraph window.

 * [finalText] starts equal to [committedText]; correction detector will update later (1.11+).

 */

data class WordSession_v1(

    val slotId: WordSlotId_v1,

    val sessionGeneration: Long,

    val inputMode: WordSessionInputMode_v1,

    val commitType: WordSessionCommitType_v1,

    val committedText: String,

    val finalText: String,

    val typedText: String,

    val separatorCharCount: Int,

    val committedAtMs: Long,

    /** Stub: polyline point count when stroke capture lands (no storage yet). */

    val swipeTracePointCount: Int = 0,

    /** Stub: tap key event count when key capture lands. */

    val tapKeyCount: Int = 0,

    /** Stub: runner-up words from suggestion strip (empty until decode pipeline). */

    val runnerUpWords: List<String> = emptyList(),

) {

    fun debugOneLine(): String = buildString {

        append("WordSlot#").append(slotId.value)

        append(' ').append(inputMode.name.lowercase())

        append(' ').append(commitType.name.lowercase())

        append(" committed=").append(committedText)

        if (typedText != committedText) append(" typed=").append(typedText)

    }

}

