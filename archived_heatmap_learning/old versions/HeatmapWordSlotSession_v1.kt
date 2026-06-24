// SPDX-License-Identifier: GPL-3.0-only
// ai-note: IME session: word slots + paragraph journal driven by Heatmap Smart Keyboard prefs
package helium314.keyboard.heatmap.learning

import helium314.keyboard.latin.BuildConfig
import helium314.keyboard.latin.settings.DebugSettings
import helium314.keyboard.latin.utils.Log
import helium314.keyboard.latin.utils.prefs
import android.content.Context
import java.util.concurrent.atomic.AtomicReference

object HeatmapWordSlotSession_v1 {
    private const val TAG = "HeatmapWordSlot"
    /** Always logged on commit so logcat works without Debug mode (beta instrumentation). */
    private const val TAG_ALWAYS = "HeatmapInstr"

    private val allocator = WordSlotAllocator_v1()
    private val journal = ParagraphJournal_v1()

    private val lastCommittedSlot = AtomicReference<WordSlotId_v1?>(null)
    private val lastCommittedWord = AtomicReference<String?>(null)

    @JvmStatic
    fun resetSession(context: Context) {
        refreshFromSettings(context)
        allocator.resetSession()
        journal.clear()
        lastCommittedSlot.set(null)
        lastCommittedWord.set(null)
        logDebug { "session reset ${HeatmapLearningBuildInfo_v1.statusLine()}" }
    }

    @JvmStatic
    fun refreshFromSettings(context: Context) {
        refreshDebugLoggingFlag(context)
        journal.setMaxChars(HeatmapLearningSettings_v1.getParagraphWindowChars(context))
    }

    @JvmStatic
    fun onComposingStarted(context: Context) {
        if (!HeatmapLearningSettings_v1.isLearningEnabled(context)) return
        refreshFromSettings(context)
        val slot = allocator.onComposingStarted()
        logDebug { "composing started $slot" }
    }

    @JvmStatic
    fun onWordCommitted(context: Context, chosenWord: String?, separatorCharCount: Int) {
        if (!HeatmapLearningSettings_v1.isLearningEnabled(context)) {
            Log.i(TAG_ALWAYS, "skip commit (learning off) word=${chosenWord ?: "?"}")
            return
        }
        refreshFromSettings(context)
        val slot = allocator.onWordCommitted()
        lastCommittedSlot.set(slot)
        lastCommittedWord.set(chosenWord)
        journal.appendCommittedWord(slot, chosenWord ?: "", separatorCharCount)
        val summary = liveSummary()
        HeatmapInstrumentationSnapshot_v2.save(context, summary)
        HeatmapImeHeartbeat_v1.incrementCommitCount(context)
        Log.i(
            TAG_ALWAYS,
            "commit $slot word=${chosenWord ?: "?"} pkg=${context.packageName} beta=${BuildConfig.HEATMAP_LEARNING_BETA} " +
                "journal=${summary.journalUsedChars}/${summary.journalMaxChars}",
        )
        logDebug {
            "committed $slot word=${chosenWord ?: "?"} journal=${journal.usedChars()}/${journal.maxChars()} " +
                "entries=${journal.entryCount()}"
        }
    }

    @JvmStatic
    fun lastCommittedSlotId(): Int? = lastCommittedSlot.get()?.value

    @JvmStatic
    fun activeComposingSlotId(): Int? = allocator.activeComposingSlot()?.value

    @JvmStatic
    fun lastCommittedWord(): String? = lastCommittedWord.get()

    @JvmStatic
    fun totalCommittedWords(): Int = allocator.totalCommittedWords()

    @JvmStatic
    fun sessionGeneration(): Long = allocator.sessionGeneration()

    @JvmStatic
    fun journalUsedChars(): Int = journal.usedChars()

    @JvmStatic
    fun journalMaxChars(): Int = journal.maxChars()

    @JvmStatic
    fun liveSummary(): HeatmapInstrumentationSnapshot_v2.LiveSummary =
        HeatmapInstrumentationSnapshot_v2.LiveSummary(
            sessionGeneration = allocator.sessionGeneration(),
            wordsCommitted = allocator.totalCommittedWords(),
            activeSlotId = allocator.activeComposingSlot()?.value,
            lastSlotId = lastCommittedSlot.get()?.value,
            lastWord = lastCommittedWord.get(),
            journalUsedChars = journal.usedChars(),
            journalMaxChars = journal.maxChars(),
            journalWordCount = journal.entryCount(),
        )

    @JvmStatic
    fun debugSummary(context: Context): String =
        HeatmapInstrumentationSnapshot_v2.formatForDebugSummary(context, liveSummary())

    private inline fun logDebug(message: () -> String) {
        if (!debugLoggingEnabled) return
        Log.d(TAG, message())
    }

    private var debugLoggingEnabled: Boolean = false

    @JvmStatic
    fun refreshDebugLoggingFlag(context: Context) {
        debugLoggingEnabled = context.prefs().getBoolean(DebugSettings.PREF_DEBUG_MODE, false)
    }
}
