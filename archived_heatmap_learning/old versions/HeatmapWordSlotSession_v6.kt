// SPDX-License-Identifier: GPL-3.0-only

// ai-note: v6 — intent path memory + v3 snapshot/path capture

package helium314.keyboard.heatmap.learning



import android.content.Context

import android.view.inputmethod.EditorInfo

import android.os.SystemClock

import helium314.keyboard.latin.BuildConfig

import helium314.keyboard.latin.RichInputConnection

import helium314.keyboard.latin.settings.DebugSettings

import helium314.keyboard.latin.utils.Log

import helium314.keyboard.latin.utils.prefs

import java.util.concurrent.atomic.AtomicReference



object HeatmapWordSlotSession_v6 {

    private const val TAG = "HeatmapWordSlot"

    private const val TAG_ALWAYS = "HeatmapInstr"

    /** EditorInfo.packageName null in some WebViews — stable sentinel for host tracking */

    const val HOST_UNKNOWN_SENTINEL: String = "<unknown-host>"



    private val allocator = WordSlotAllocator_v1()

    private val journal = ParagraphJournal_v2()

    private val wordSessions = WordSessionRegistry_v5()



    private val lastCommittedSlot = AtomicReference<WordSlotId_v1?>(null)

    private val lastCommittedWord = AtomicReference<String?>(null)

    private val lastWordSession = AtomicReference<WordSession_v5?>(null)

    @Volatile

    private var lastHostKey: String? = null

    @Volatile

    private var fieldLearningAllowed: Boolean = true



    @JvmStatic

    @JvmOverloads

    fun onInputViewStarted(

        context: Context,

        hostPackage: String?,

        isDifferentTextField: Boolean,

        editorInfo: EditorInfo? = null,

    ) {

        val previousFieldLearningAllowed = fieldLearningAllowed

        fieldLearningAllowed = HeatmapLearningGate_v1.shouldRecord(context, editorInfo)

        if (hostPackage != null && hostPackage == context.packageName) return

        val hostKey = hostPackage ?: HOST_UNKNOWN_SENTINEL

        val hostChanged = hostKey != lastHostKey

        if (hostChanged && lastHostKey != null && previousFieldLearningAllowed) {

            triggerFlush(context, CorrectionFlushReason_v1.HOST_CHANGED, hostPackage, null, null)

        }

        if (hostChanged) lastHostKey = hostKey

        if (hostChanged || isDifferentTextField) {

            if (!hostChanged && isDifferentTextField && previousFieldLearningAllowed) {

                triggerFlush(context, CorrectionFlushReason_v1.SESSION_RESET, hostPackage, null, null)

            }

            resetSessionInternal(context)

            logDebug {

                "input view host=$hostKey hostChanged=$hostChanged differentField=$isDifferentTextField " +

                    "learningAllowed=$fieldLearningAllowed skip=${HeatmapLearningGate_v1.skipReason(editorInfo, context)}"

            }

        }

    }



    @JvmStatic

    fun onFieldBlur(context: Context, hostPackage: String?) {

        triggerFlush(context, CorrectionFlushReason_v1.FIELD_BLUR, hostPackage, null, null)

    }



    @JvmStatic

    @JvmOverloads

    fun onFieldBlur(

        context: Context,

        hostPackage: String?,

        connection: RichInputConnection?,

        editorInfo: EditorInfo?,

    ) {

        triggerFlush(context, CorrectionFlushReason_v1.FIELD_BLUR, hostPackage, connection, editorInfo)

    }



    @JvmStatic

    fun onImeAction(context: Context, hostPackage: String?) {

        triggerFlush(context, CorrectionFlushReason_v1.IME_ACTION, hostPackage, null, null)

    }



    @JvmStatic

    fun onImeAction(

        context: Context,

        hostPackage: String?,

        connection: RichInputConnection,

        editorInfo: EditorInfo?,

    ) {

        triggerFlush(context, CorrectionFlushReason_v1.IME_ACTION, hostPackage, connection, editorInfo)

    }



    @JvmStatic

    fun onSelectionChanged(

        context: Context,

        oldSelStart: Int,

        oldSelEnd: Int,

        newSelStart: Int,

        newSelEnd: Int,

    ) {

        if (!fieldLearningAllowed || !HeatmapLearningSettings_v2.isLearningEnabled(context)) return

        InWindowEditDetector_v1.onSelectionChanged(context, oldSelStart, oldSelEnd, newSelStart, newSelEnd)

    }



    internal fun markEditSuspected() {

        wordSessions.markEditSuspected()

    }



    @JvmStatic

    fun onManualDebugRefresh(context: Context) {

        triggerFlush(

            context,

            CorrectionFlushReason_v1.MANUAL_DEBUG,

            lastWordSession.get()?.hostPackage,

            null,

            null,

            ignoreFieldGate = true,

        )

    }



    @JvmStatic

    fun resetSession(context: Context) {

        triggerFlush(context, CorrectionFlushReason_v1.SESSION_RESET, lastWordSession.get()?.hostPackage, null, null)

        resetSessionInternal(context)

    }



    /** After settings “reset learning” — skip flush so we do not rewrite export JSON. */

    @JvmStatic

    fun clearInMemorySessionAfterTrainingWipe(context: Context) {

        resetSessionInternal(context)

    }



    private fun triggerFlush(

        context: Context,

        reason: CorrectionFlushReason_v1,

        hostPackage: String?,

        connection: RichInputConnection?,

        editorInfo: EditorInfo?,

        ignoreFieldGate: Boolean = false,

    ) {

        if (!HeatmapLearningSettings_v2.isLearningEnabled(context)) return

        if (!ignoreFieldGate) {

            val allowed = if (editorInfo != null) {

                HeatmapLearningGate_v1.shouldRecord(context, editorInfo)

            } else {

                fieldLearningAllowed

            }

            if (!allowed) return

        }

        val snapshot = connection?.let { FieldTextProbe_v1.capture(it, editorInfo) }

        val pending = wordSessions.pendingFinalTextSyncCount()

        InWindowEditDetector_v1.onFlushFieldProbe(context, snapshot, pending)

        CorrectionDetector_v1.onFlush(context, reason, hostPackage, flushContext())

        wordSessions.markStableAtFlush()

        HeatmapMetricsRecorder_v1.onFlush(context)

        persistExportSnapshot(context, "ime_flush_${reason.name}", archiveOnFlush = true)

    }



    private fun persistExportSnapshot(context: Context, source: String, archiveOnFlush: Boolean) {

        if (!HeatmapLearningSettings_v2.isLearningEnabled(context) || !fieldLearningAllowed) return

        val json = HeatmapInstrumentationJson_v4.build(

            context,

            wordMemory = HeatmapWordMemoryLedger_v2.copyOldestFirst(),

            sessions = wordSessions.copySessionsOldestFirst(),

            journalEntries = journal.entriesOldestFirst(),

            liveSummary = liveSummary(),

            source = source,

        )

        HeatmapSessionExportWriter_v1.writeLatest(context, json)

        if (archiveOnFlush) HeatmapSessionExportWriter_v1.writeTimestampedArchive(context, json)

    }



    private fun resetSessionInternal(context: Context) {

        refreshFromSettings(context)

        allocator.resetSession()

        journal.clear()

        wordSessions.clear()

        HeatmapWordMemoryLedger_v2.clear()

        HeatmapSwipeCorrectionChain_v1.clearPending()

        HeatmapSwipeGeometryLedger_v1.clear()

        HeatmapSwipeTraceRingBuffer_v3.clear()

        lastCommittedSlot.set(null)

        lastCommittedWord.set(null)

        lastWordSession.set(null)

        logDebug { "session reset ${HeatmapLearningBuildInfo_v1.statusLine()}" }

    }



    @JvmStatic

    fun refreshFromSettings(context: Context) {

        refreshDebugLoggingFlag(context)

        journal.setMaxChars(HeatmapLearningSettings_v2.getParagraphWindowChars(context))

        journal.setMaxWords(HeatmapLearningSettings_v1.WORD_MEMORY_MAX_WORDS)

    }



    @JvmStatic

    @JvmOverloads

    fun onComposingStarted(context: Context, editorInfo: EditorInfo? = null) {

        if (!HeatmapLearningGate_v1.shouldRecord(context, editorInfo)) return

        fieldLearningAllowed = HeatmapLearningGate_v1.shouldRecord(context, editorInfo)

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

        hostPackage: String?,

        editorInfo: EditorInfo? = null,

    ) {

        if (!HeatmapLearningGate_v1.shouldRecord(context, editorInfo)) {

            val reason = HeatmapLearningGate_v1.skipReason(editorInfo, context) ?: "blocked"

            Log.i(TAG_ALWAYS, "skip commit ($reason) word=${chosenWord ?: "?"}")

            return

        }

        fieldLearningAllowed = true

        val metricsStart = SystemClock.elapsedRealtime()

        refreshFromSettings(context)

        val slot = allocator.onWordCommitted()

        val committed = chosenWord ?: ""

        val typed = typedWord ?: committed

        val decodeSnap = if (isGestureInput) {
            helium314.keyboard.heatmap.swipe.HeatmapSwipeDecodeSnapshot_v3.consume()
        } else {
            null
        }

        val session = HeatmapWordSessionBuilder_v3.fromDecodeSnapshot(
            context = context,
            slotId = slot,
            sessionGeneration = allocator.sessionGeneration(),
            hostPackage = hostPackage,
            isGestureInput = isGestureInput,
            latinCommitType = latinCommitType,
            text = committed,
            typedText = typed,
            separatorCharCount = separatorCharCount.coerceAtLeast(0),
            outcome = WordSessionOutcome_v1.KEPT_IN_FIELD,
            memorySequence = HeatmapWordMemoryLedger_v2.nextMemorySequence(),
            linkedSlotId = null,
            snap = decodeSnap,
        )

        recordWordMemoryAttempt(context, session, archiveOnCommit = false)

        val previousWord = lastCommittedWord.get()

        HeatmapLearningStore_v1.recordWordCommit(context, session, previousWord)

        lastCommittedSlot.set(slot)

        lastCommittedWord.set(committed)

        val evicted = journal.appendWordAttempt(
            slot, committed, session.separatorCharCount, WordSessionOutcome_v1.KEPT_IN_FIELD,
        )

        if (evicted > 0) {

            triggerFlush(context, CorrectionFlushReason_v1.WINDOW_SLIDE, hostPackage, null, null)

        }

        val summary = liveSummary()

        HeatmapInstrumentationSnapshot_v7.save(context, summary, session)

        val acRoute = if (HeatmapAutocorrectPolicy_v1.useHeliBoardAutocorrectForMode(context, session.inputMode)) {

            "heliboard-ac"

        } else {

            "heatmap-ac"

        }

        Log.i(

            TAG_ALWAYS,

            "commit ${session.debugOneLine()} route=$acRoute ime=${context.packageName} " +

                "beta=${BuildConfig.HEATMAP_LEARNING_BETA} journal=${summary.journalUsedChars}/${summary.journalMaxChars} " +
                "memory=${HeatmapWordMemoryLedger_v2.entryCount()}/${HeatmapWordMemoryLedger_v2.maxWordEntries()} " +
                "kept=${HeatmapWordMemoryLedger_v2.keptCount()} erased=${HeatmapWordMemoryLedger_v2.erasedCount()}",

        )

        logDebug {

            "committed $session journal=${journal.usedChars()}/${journal.maxChars()} entries=${journal.entryCount()}"

        }

        persistExportSnapshot(context, "ime_commit", archiveOnFlush = false)

        HeatmapMetricsRecorder_v1.onCommitPathFinished(context, SystemClock.elapsedRealtime() - metricsStart)

    }



    @JvmStatic

    fun lastExportStatusLine(context: Context): String =

        HeatmapSessionExportWriter_v1.formatLastExportLine(context)



    fun flushContext(): CorrectionFlushContext_v1 = CorrectionFlushContext_v1(

        sessionGeneration = allocator.sessionGeneration(),

        journalWordCount = journal.entryCount(),

        registrySessionCount = wordSessions.size(),

        journalUsedChars = journal.usedChars(),

    )



    @JvmStatic

    fun lastWordSession(): WordSession_v5? = lastWordSession.get()



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

    fun liveSummary(): HeatmapInstrumentationSnapshot_v6.LiveSummary =

        HeatmapInstrumentationSnapshot_v6.LiveSummary(

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

            lastHostPackage = lastWordSession.get()?.hostPackage,

        )



    @JvmStatic

    fun debugSummary(context: Context): String =

        HeatmapInstrumentationSnapshot_v10.formatForDebugSummary(context, liveSummary())



    private fun recordWordMemoryAttempt(
        context: Context,
        session: WordSession_v5,
        archiveOnCommit: Boolean,
    ) {
        var enriched = session
        when (session.wordMemoryOutcome) {
            WordSessionOutcome_v1.KEPT_IN_FIELD -> {
                val resolved = when (session.inputMode) {
                    WordSessionInputMode_v1.SWIPE ->
                        HeatmapSwipeCorrectionChain_v1.onKeptWord(session)
                    WordSessionInputMode_v1.TAP ->
                        HeatmapSwipeCorrectionChain_v1.onKeptTapWord(
                            session.committedText, session.memorySequence,
                        )
                }
                if (resolved != null) {
                    HeatmapSwipeGeometryLedger_v1.record(resolved)
                    enriched = session.copy(
                        correctionChainId = resolved.chainId,
                        correctionAttemptIndex = resolved.attempts.lastIndex,
                        correctionFinalWord = resolved.finalWord,
                    )
                }
            }
            WordSessionOutcome_v1.ERASED_BEFORE_COMMIT,
            WordSessionOutcome_v1.ERASED_FROM_FIELD,
            WordSessionOutcome_v1.REVERTED_FROM_FIELD,
            -> {
                if (session.inputMode == WordSessionInputMode_v1.SWIPE) {
                    HeatmapSwipeCorrectionChain_v1.onErasedAttempt(session)
                }
            }
        }
        wordSessions.record(enriched)
        HeatmapWordMemoryLedger_v2.record(enriched)
        if (enriched.inputMode == WordSessionInputMode_v1.SWIPE) {
            HeatmapSwipeTraceRingBuffer_v3.record(enriched)
        }
        lastWordSession.set(enriched)
    }

    /** ai-note: batch swipe rejected via backspace before field commit — full trace kept */
    @JvmStatic
    fun onBatchComposingRejected(
        context: Context,
        rejectedWord: String?,
        isGestureInput: Boolean,
        hostPackage: String?,
        editorInfo: EditorInfo? = null,
    ) {
        if (!HeatmapLearningGate_v1.shouldRecord(context, editorInfo)) return
        if (rejectedWord.isNullOrEmpty()) return
        refreshFromSettings(context)
        val slot = allocator.activeComposingSlot() ?: WordSlotId_v1(0)
        val snap = helium314.keyboard.heatmap.swipe.HeatmapSwipeDecodeSnapshot_v3.peek()
        val linked = lastWordSession.get()?.slotId
        val session = HeatmapWordSessionBuilder_v3.fromDecodeSnapshot(
            context = context,
            slotId = slot,
            sessionGeneration = allocator.sessionGeneration(),
            hostPackage = hostPackage,
            isGestureInput = isGestureInput,
            latinCommitType = helium314.keyboard.latin.LastComposedWord.COMMIT_TYPE_USER_TYPED_WORD,
            text = rejectedWord,
            typedText = rejectedWord,
            separatorCharCount = 0,
            outcome = WordSessionOutcome_v1.ERASED_BEFORE_COMMIT,
            memorySequence = HeatmapWordMemoryLedger_v2.nextMemorySequence(),
            linkedSlotId = linked,
            snap = snap,
        )
        recordWordMemoryAttempt(context, session, archiveOnCommit = false)
        journal.appendWordAttempt(
            slot, rejectedWord, 0, WordSessionOutcome_v1.ERASED_BEFORE_COMMIT,
        )
        helium314.keyboard.heatmap.swipe.HeatmapSwipeSlotRejectMemory_v7
            .recordFromDecodeSnapshot(rejectedWord)
        Log.i(TAG_ALWAYS, "memory erased-before-commit ${session.debugOneLine()}")
        persistExportSnapshot(context, "ime_swipe_rejected", archiveOnFlush = false)
    }

    /** ai-note: committed swipe word reverted via backspace (autocorrect revert path) */
    @JvmStatic
    fun onCommittedWordReverted(
        context: Context,
        hostPackage: String?,
        editorInfo: EditorInfo? = null,
    ) {
        if (!HeatmapLearningGate_v1.shouldRecord(context, editorInfo)) return
        val prior = lastWordSession.get() ?: return
        recordErasedCopy(
            context, prior, WordSessionOutcome_v1.REVERTED_FROM_FIELD, hostPackage,
        )
        helium314.keyboard.heatmap.swipe.HeatmapSwipeSlotRejectMemory_v7
            .recordFromLastSession(prior)
    }

    /** ai-note: user backspaced through a committed word in the text field */
    @JvmStatic
    fun onFieldWordDeleted(
        context: Context,
        deletedWord: String?,
        hostPackage: String?,
        editorInfo: EditorInfo? = null,
    ) {
        if (!HeatmapLearningGate_v1.shouldRecord(context, editorInfo)) return
        if (deletedWord.isNullOrEmpty()) return
        val prior = lastWordSession.get() ?: return
        if (!deletedWord.equals(prior.committedText, ignoreCase = true)) return
        recordErasedCopy(
            context, prior, WordSessionOutcome_v1.ERASED_FROM_FIELD, hostPackage,
        )
        helium314.keyboard.heatmap.swipe.HeatmapSwipeSlotRejectMemory_v7
            .recordFromLastSession(prior)
    }

    private fun recordErasedCopy(
        context: Context,
        prior: WordSession_v5,
        outcome: WordSessionOutcome_v1,
        hostPackage: String?,
    ) {
        refreshFromSettings(context)
        val session = prior.copy(
            committedAtMs = System.currentTimeMillis(),
            wordMemoryOutcome = outcome,
            memorySequence = HeatmapWordMemoryLedger_v2.nextMemorySequence(),
            linkedSlotId = prior.slotId,
            hostPackage = hostPackage ?: prior.hostPackage,
        )
        recordWordMemoryAttempt(context, session, archiveOnCommit = false)
        journal.appendWordAttempt(
            prior.slotId, prior.committedText, prior.separatorCharCount, outcome,
        )
        Log.i(TAG_ALWAYS, "memory ${outcome.name.lowercase()} ${session.debugOneLine()}")
        persistExportSnapshot(context, "ime_word_erased", archiveOnFlush = false)
    }

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

