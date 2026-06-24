// SPDX-License-Identifier: GPL-3.0-only
// ai-note: step 1.8–1.9 — WordSession registry + commitType/mode on commit
package helium314.keyboard.heatmap.learning

import android.content.Context
import helium314.keyboard.latin.BuildConfig
import helium314.keyboard.latin.settings.DebugSettings
import helium314.keyboard.latin.utils.Log
import helium314.keyboard.latin.utils.prefs
import java.util.concurrent.atomic.AtomicReference

object HeatmapWordSlotSession_v2 {
    private const val TAG = "HeatmapWordSlot"
    private const val TAG_ALWAYS = "HeatmapInstr"

    private val allocator = WordSlotAllocator_v1()
    private val journal = ParagraphJournal_v1()
    private val wordSessions = WordSessionRegistry_v1()

    private val lastCommittedSlot = AtomicReference<WordSlotId_v1?>(null)
    private val lastCommittedWord = AtomicReference<String?>(null)
    private val lastWordSession = AtomicReference<WordSession_v1?>(null)

    @JvmStatic
    fun resetSession(context: Context) {
        refreshFromSettings(context)
        allocator.resetSession()
        journal.clear()
        wordSessions.clear()
        lastCommittedSlot.set(null)
        lastCommittedWord.set(null)
        lastWordSession.set(null)
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
    fun onWordCommitted(
        context: Context,
        chosenWord: String?,
        separatorCharCount: Int,
        latinCommitType: Int,
        isGestureInput: Boolean,
        typedWord: String?,
    ) {
        if (!HeatmapLearningSettings_v1.isLearningEnabled(context)) {
            Log.i(TAG_ALWAYS, "skip commit (learning off) word=${chosenWord ?: "?"}")
            return
        }
        refreshFromSettings(context)
        val slot = allocator.onWordCommitted()
        val committed = chosenWord ?: ""
        val typed = typedWord ?: committed
        val session = WordSession_v1(
            slotId = slot,
            sessionGeneration = allocator.sessionGeneration(),
            inputMode = WordSessionInputMode_v1.fromGestureInput(isGestureInput),
            commitType = WordSessionCommitType_v1.fromLatinCommitType(latinCommitType),
            committedText = committed,
            finalText = committed,
            typedText = typed,
            separatorCharCount = separatorCharCount.coerceAtLeast(0),
            committedAtMs = System.currentTimeMillis(),
        )
        wordSessions.record(session)
        lastCommittedSlot.set(slot)
        lastCommittedWord.set(committed)
        lastWordSession.set(session)
        journal.appendCommittedWord(slot, committed, session.separatorCharCount)
        val summary = liveSummary()
        HeatmapInstrumentationSnapshot_v3.save(context, summary, session)
        HeatmapImeHeartbeat_v1.incrementCommitCount(context)
        Log.i(
            TAG_ALWAYS,
            "commit ${session.debugOneLine()} pkg=${context.packageName} beta=${BuildConfig.HEATMAP_LEARNING_BETA} " +
                "journal=${summary.journalUsedChars}/${summary.journalMaxChars}",
        )
        logDebug {
            "committed $session journal=${journal.usedChars()}/${journal.maxChars()} entries=${journal.entryCount()}"
        }
    }

    @JvmStatic
    fun lastWordSession(): WordSession_v1? = lastWordSession.get()

    @JvmStatic
    fun wordSessionCount(): Int = wordSessions.size()

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
    fun liveSummary(): HeatmapInstrumentationSnapshot_v3.LiveSummary =
        HeatmapInstrumentationSnapshot_v3.LiveSummary(
            sessionGeneration = allocator.sessionGeneration(),
            wordsCommitted = allocator.totalCommittedWords(),
            activeSlotId = allocator.activeComposingSlot()?.value,
            lastSlotId = lastCommittedSlot.get()?.value,
            lastWord = lastCommittedWord.get(),
            journalUsedChars = journal.usedChars(),
            journalMaxChars = journal.maxChars(),
            journalWordCount = journal.entryCount(),
            lastInputMode = lastWordSession.get()?.inputMode?.name,
            lastCommitType = lastWordSession.get()?.commitType?.name,
            lastTypedWord = lastWordSession.get()?.typedText,
        )

    @JvmStatic
    fun debugSummary(context: Context): String =
        HeatmapInstrumentationSnapshot_v3.formatForDebugSummary(context, liveSummary())

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
