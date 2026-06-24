// SPDX-License-Identifier: GPL-3.0-only
// ai-note: v3 adds WordSession input mode + commit type to cross-process status snapshot
package helium314.keyboard.heatmap.learning

import android.content.Context
import android.content.SharedPreferences

object HeatmapInstrumentationSnapshot_v3 {
    private const val PREF_SNAPSHOT_WORDS = "heatmap_snapshot_words_committed"
    private const val PREF_SNAPSHOT_JOURNAL_CHARS = "heatmap_snapshot_journal_chars"
    private const val PREF_SNAPSHOT_JOURNAL_WORDS = "heatmap_snapshot_journal_words"
    private const val PREF_SNAPSHOT_LAST_WORD = "heatmap_snapshot_last_word"
    private const val PREF_SNAPSHOT_LAST_SLOT = "heatmap_snapshot_last_slot"
    private const val PREF_SNAPSHOT_SESSION_GEN = "heatmap_snapshot_session_gen"
    private const val PREF_SNAPSHOT_INPUT_MODE = "heatmap_snapshot_input_mode"
    private const val PREF_SNAPSHOT_COMMIT_TYPE = "heatmap_snapshot_commit_type"
    private const val PREF_SNAPSHOT_TYPED_WORD = "heatmap_snapshot_typed_word"

    fun save(context: Context, summary: LiveSummary, lastSession: WordSession_v1?) {
        HeatmapCrossProcessPrefs_v1.editCommit(context) {
            putInt(PREF_SNAPSHOT_WORDS, summary.wordsCommitted)
            putInt(PREF_SNAPSHOT_JOURNAL_CHARS, summary.journalUsedChars)
            putInt(PREF_SNAPSHOT_JOURNAL_WORDS, summary.journalWordCount)
            putString(PREF_SNAPSHOT_LAST_WORD, summary.lastWord)
            putInt(PREF_SNAPSHOT_LAST_SLOT, summary.lastSlotId ?: -1)
            putLong(PREF_SNAPSHOT_SESSION_GEN, summary.sessionGeneration)
            putString(PREF_SNAPSHOT_INPUT_MODE, lastSession?.inputMode?.name)
            putString(PREF_SNAPSHOT_COMMIT_TYPE, lastSession?.commitType?.name)
            putString(PREF_SNAPSHOT_TYPED_WORD, lastSession?.typedText)
        }
    }

    fun formatForDebugSummary(context: Context, @Suppress("UNUSED_PARAMETER") liveInThisProcess: LiveSummary): String {
        val snap = read(context)
        return buildString {
            append(HeatmapLearningBuildInfo_v1.statusLine())
            append("\n").append(HeatmapImeHeartbeat_v1.formatStatusLine(context))
            append("\nLearning enabled: ").append(HeatmapLearningSettings_v1.isLearningEnabled(context))
            append("\n\n— Last commit from IME (tap status row to refresh) —")
            if (snap.wordsCommitted <= 0) {
                append("\nNo commits saved yet.")
                append("\nUse Heatmap KB (beta) in another app, type words with space, then return here.")
            } else {
                append("\nSession gen: ").append(snap.sessionGeneration)
                append("\nWords committed: ").append(snap.wordsCommitted)
                append("\nLast slot: ").append(snap.lastSlotId?.let { "WordSlot#$it" } ?: "none")
                append("\nInput mode: ").append(snap.inputMode ?: "unknown")
                append("\nCommit type: ").append(snap.commitType ?: "unknown")
                append("\nCommitted: ").append(snap.lastWord ?: "none")
                if (!snap.typedWord.isNullOrEmpty() && snap.typedWord != snap.lastWord) {
                    append("\nTyped (before commit): ").append(snap.typedWord)
                }
                append("\nJournal: ").append(snap.journalUsedChars).append(" chars, ")
                    .append(snap.journalWordCount).append(" words")
            }
            append("\n\n— Live IME session —")
            append("\n").append(liveSectionNote())
        }
    }

    private fun liveSectionNote(): String =
        "Not shown here: the keyboard runs in a separate process from Settings. " +
            "After you type elsewhere, use “Last commit from IME” above (tap this row to refresh)."

    private fun read(prefs: SharedPreferences): Snapshot {
        return Snapshot(
            wordsCommitted = prefs.getInt(PREF_SNAPSHOT_WORDS, 0),
            journalUsedChars = prefs.getInt(PREF_SNAPSHOT_JOURNAL_CHARS, 0),
            journalWordCount = prefs.getInt(PREF_SNAPSHOT_JOURNAL_WORDS, 0),
            lastWord = prefs.getString(PREF_SNAPSHOT_LAST_WORD, null),
            lastSlotId = prefs.getInt(PREF_SNAPSHOT_LAST_SLOT, -1).takeIf { it >= 0 },
            sessionGeneration = prefs.getLong(PREF_SNAPSHOT_SESSION_GEN, 0L),
            inputMode = prefs.getString(PREF_SNAPSHOT_INPUT_MODE, null),
            commitType = prefs.getString(PREF_SNAPSHOT_COMMIT_TYPE, null),
            typedWord = prefs.getString(PREF_SNAPSHOT_TYPED_WORD, null),
        )
    }

    private fun read(context: Context): Snapshot = read(HeatmapCrossProcessPrefs_v1.readPrefs(context))

    data class LiveSummary(
        val sessionGeneration: Long,
        val wordsCommitted: Int,
        val activeSlotId: Int?,
        val lastSlotId: Int?,
        val lastWord: String?,
        val journalUsedChars: Int,
        val journalMaxChars: Int,
        val journalWordCount: Int,
        val lastInputMode: String?,
        val lastCommitType: String?,
        val lastTypedWord: String?,
    )

    private data class Snapshot(
        val wordsCommitted: Int,
        val journalUsedChars: Int,
        val journalWordCount: Int,
        val lastWord: String?,
        val lastSlotId: Int?,
        val sessionGeneration: Long,
        val inputMode: String?,
        val commitType: String?,
        val typedWord: String?,
    )
}
