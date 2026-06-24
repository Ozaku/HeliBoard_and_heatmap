// SPDX-License-Identifier: GPL-3.0-only
// ai-note: persists last commit stats so settings UI survives session reset when opening our app
package helium314.keyboard.heatmap.learning

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import helium314.keyboard.latin.utils.prefs

object HeatmapInstrumentationSnapshot_v1 {
    private const val PREF_SNAPSHOT_WORDS = "heatmap_snapshot_words_committed"
    private const val PREF_SNAPSHOT_JOURNAL_CHARS = "heatmap_snapshot_journal_chars"
    private const val PREF_SNAPSHOT_JOURNAL_WORDS = "heatmap_snapshot_journal_words"
    private const val PREF_SNAPSHOT_LAST_WORD = "heatmap_snapshot_last_word"
    private const val PREF_SNAPSHOT_LAST_SLOT = "heatmap_snapshot_last_slot"
    private const val PREF_SNAPSHOT_SESSION_GEN = "heatmap_snapshot_session_gen"

    fun save(context: Context, summary: LiveSummary) {
        context.prefs().edit {
            putInt(PREF_SNAPSHOT_WORDS, summary.wordsCommitted)
            putInt(PREF_SNAPSHOT_JOURNAL_CHARS, summary.journalUsedChars)
            putInt(PREF_SNAPSHOT_JOURNAL_WORDS, summary.journalWordCount)
            putString(PREF_SNAPSHOT_LAST_WORD, summary.lastWord)
            putInt(PREF_SNAPSHOT_LAST_SLOT, summary.lastSlotId ?: -1)
            putLong(PREF_SNAPSHOT_SESSION_GEN, summary.sessionGeneration)
        }
    }

    fun formatForDebugSummary(context: Context, live: LiveSummary): String {
        val snap = read(context)
        return buildString {
            append(HeatmapLearningBuildInfo_v1.statusLine())
            append("\n").append(HeatmapImeHeartbeat_v1.formatStatusLine(context))
            append("\nLearning enabled: ").append(HeatmapLearningSettings_v1.isLearningEnabled(context))
            append("\n\n— Live IME session (non-zero only while keyboard is open to a text field) —")
            append("\nSession gen: ").append(live.sessionGeneration)
            append("\nWords committed: ").append(live.wordsCommitted)
            append("\nActive composing slot: ").append(live.activeSlotId?.let { "WordSlot#$it" } ?: "none")
            append("\nLast committed slot: ").append(live.lastSlotId?.let { "WordSlot#$it" } ?: "none")
            append("\nLast word: ").append(live.lastWord ?: "none")
            append("\nJournal: ").append(live.journalUsedChars).append(" / ")
                .append(live.journalMaxChars).append(" chars, ")
                .append(live.journalWordCount).append(" words")
            if (snap.wordsCommitted > 0) {
                append("\n\n— Last saved snapshot (after your last commit) —")
                append("\nSession gen: ").append(snap.sessionGeneration)
                append("\nWords committed: ").append(snap.wordsCommitted)
                append("\nLast slot: ").append(
                    snap.lastSlotId?.let { "WordSlot#$it" } ?: "none"
                )
                append("\nLast word: ").append(snap.lastWord ?: "none")
                append("\nJournal: ").append(snap.journalUsedChars).append(" chars, ")
                    .append(snap.journalWordCount).append(" words")
            }
        }
    }

    private fun read(prefs: SharedPreferences): Snapshot {
        return Snapshot(
            wordsCommitted = prefs.getInt(PREF_SNAPSHOT_WORDS, 0),
            journalUsedChars = prefs.getInt(PREF_SNAPSHOT_JOURNAL_CHARS, 0),
            journalWordCount = prefs.getInt(PREF_SNAPSHOT_JOURNAL_WORDS, 0),
            lastWord = prefs.getString(PREF_SNAPSHOT_LAST_WORD, null),
            lastSlotId = prefs.getInt(PREF_SNAPSHOT_LAST_SLOT, -1).takeIf { it >= 0 },
            sessionGeneration = prefs.getLong(PREF_SNAPSHOT_SESSION_GEN, 0L),
        )
    }

    private fun read(context: Context): Snapshot = read(context.prefs())

    data class LiveSummary(
        val sessionGeneration: Long,
        val wordsCommitted: Int,
        val activeSlotId: Int?,
        val lastSlotId: Int?,
        val lastWord: String?,
        val journalUsedChars: Int,
        val journalMaxChars: Int,
        val journalWordCount: Int,
    )

    private data class Snapshot(
        val wordsCommitted: Int,
        val journalUsedChars: Int,
        val journalWordCount: Int,
        val lastWord: String?,
        val lastSlotId: Int?,
        val sessionGeneration: Long,
    )
}
