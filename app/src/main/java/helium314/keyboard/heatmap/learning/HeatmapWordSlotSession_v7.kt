// SPDX-License-Identifier: GPL-3.0-only

// ai-note: v7 — POSITION-AWARE session manager. Supersedes v6. Owns HeatmapFieldWordModel_v1 (the
// offset backbone) and swaps in the position-aware pieces:
//   allocator  -> WordSlotAllocator_v2          (reuse slot when re-editing an existing position)
//   chains     -> HeatmapSwipeCorrectionChain_v2 (gated grouping + orphan bucket; breaks way->did)
//   builder    -> HeatmapWordSessionBuilder_v4   (anchor geometry + offsets/coherence stamped)
//   export     -> HeatmapInstrumentationJson_v6  (schema 6: + swipeAnchors / strict candidate count)
//   finalText  -> InWindowEditDetector_v2 + FieldTextProbe_v2 (real reconciliation at flush)
// Earlier-word edits/deletes are attributed by OFFSET via the field model (v6 only matched the last
// committed word); trailing-delete-to-fix-earlier and far cursor jumps set possibleMindChange /
// proofreadEdit. Public API mirrors v6 so existing callers only swap the symbol; offset-carrying
// overloads are added where the IME can supply mConnection.getExpectedSelectionStart().
// Thread-safe sinks via AtomicReference, same as v6.
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

object HeatmapWordSlotSession_v7 {

    private const val TAG = "HeatmapWordSlot"
    private const val TAG_ALWAYS = "HeatmapInstr"

    /** EditorInfo.packageName null in some WebViews — stable sentinel for host tracking */
    const val HOST_UNKNOWN_SENTINEL: String = "<unknown-host>"

    private val allocator = WordSlotAllocator_v2()
    private val journal = ParagraphJournal_v2()
    private val wordSessions = WordSessionRegistry_v5()
    private val fieldModel = HeatmapFieldWordModel_v1()

    private val lastCommittedSlot = AtomicReference<WordSlotId_v1?>(null)
    private val lastCommittedWord = AtomicReference<String?>(null)
    private val lastWordSession = AtomicReference<WordSession_v5?>(null)

    @Volatile private var lastCommitEndOffset: Int = -1
    @Volatile private var lastHostKey: String? = null
    @Volatile private var fieldLearningAllowed: Boolean = true

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
        // ai-note: keep the field model's live cursor in sync for position-aware slot reuse / edits.
        fieldModel.onSelectionMoved(newSelStart, newSelEnd)
        // ai-note: a jump far from the last commit is a proofreading move — flag the word landed on.
        if (InWindowEditDetector_v2.classifyCursorJump(lastCommitEndOffset, newSelStart)) {
            fieldModel.wordAtOffset(newSelStart)?.let { fw ->
                wordSessions.updateLastBySlot(fw.slotId) { it.copy(proofreadEdit = true) }
            }
        }
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

    /** After settings "reset learning" — skip flush so we do not rewrite export JSON. */
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
        // ai-note: reconcile each tracked slot's finalText against the word now in the field.
        val window = FieldTextProbe_v2.capture(connection, editorInfo)
        InWindowEditDetector_v2.reconcileFinalText(fieldModel, window, wordSessions)
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
        val json = HeatmapInstrumentationJson_v6.build(
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
        fieldModel.clear()
        HeatmapWordMemoryLedger_v2.clear()
        HeatmapSwipeCorrectionChain_v2.clearAll()
        HeatmapSwipeTraceRingBuffer_v3.clear()
        lastCommittedSlot.set(null)
        lastCommittedWord.set(null)
        lastWordSession.set(null)
        lastCommitEndOffset = -1
        logDebug { "session reset ${HeatmapLearningBuildInfo_v1.statusLine()}" }
    }

    @JvmStatic
    fun refreshFromSettings(context: Context) {
        refreshDebugLoggingFlag(context)
        journal.setMaxChars(HeatmapLearningSettings_v2.getParagraphWindowChars(context))
        journal.setMaxWords(HeatmapLearningSettings_v1.WORD_MEMORY_MAX_WORDS)
        HeatmapUserProfile_v1.load(context)
    }

    @JvmStatic
    @JvmOverloads
    fun onComposingStarted(context: Context, editorInfo: EditorInfo? = null) =
        onComposingStarted(context, -1, editorInfo)

    @JvmStatic
    fun onComposingStarted(context: Context, composingStartOffset: Int, editorInfo: EditorInfo?) {
        if (!HeatmapLearningGate_v1.shouldRecord(context, editorInfo)) return
        fieldLearningAllowed = true
        refreshFromSettings(context)
        val slot = allocator.onComposingStarted(composingStartOffset, fieldModel)
        logDebug { "composing started $slot offset=$composingStartOffset reused=${allocator.reusedExistingSlot()}" }
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
        // ai-note: capture position BEFORE finalizing the slot (allocator clears it on commit).
        val composingStart = allocator.activeComposingStartOffset()
        val reusedSlot = allocator.reusedExistingSlot()
        val slot = allocator.onWordCommitted()
        val committed = chosenWord ?: ""
        val typed = typedWord ?: committed
        val committedLen = committed.length
        val endOffset = if (composingStart >= 0) composingStart + committedLen else -1
        val coherence = if (composingStart >= 0) {
            WordSessionCoherence_v1.CLEAN
        } else {
            WordSessionCoherence_v1.SUSPECT
        }
        val proofread = reusedSlot &&
            InWindowEditDetector_v2.classifyCursorJump(lastCommitEndOffset, composingStart)

        // ai-note: re-edit of an existing position supersedes that slot's prior session.
        if (reusedSlot) {
            wordSessions.updateLastBySlot(slot) {
                it.copy(
                    wordMemoryOutcome = WordSessionOutcome_v1.SUPERSEDED_BY_LATER_EDIT,
                    supersededBySlotId = slot,
                )
            }
        }

        val decodeSnap = if (isGestureInput) {
            helium314.keyboard.heatmap.swipe.HeatmapSwipeDecodeSnapshot_v3.consume()
        } else {
            null
        }

        var session = HeatmapWordSessionBuilder_v4.fromDecodeSnapshot(
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
            fieldStartOffset = composingStart,
            fieldEndOffset = endOffset,
            coherence = coherence,
        )
        if (proofread) session = session.copy(proofreadEdit = true)

        // ai-note: register/refresh this word's offset range in the precision backbone.
        fieldModel.onCommit(
            word = committed,
            composingStart = composingStart,
            committedLen = committedLen,
            slotId = slot,
            inputMode = session.inputMode,
            memorySequence = session.memorySequence,
            committedAtMs = session.committedAtMs,
        )

        recordWordMemoryAttempt(context, session)

        val previousWord = lastCommittedWord.get()
        HeatmapLearningStore_v1.recordWordCommit(context, session, previousWord)
        lastCommittedSlot.set(slot)
        lastCommittedWord.set(committed)
        lastCommitEndOffset = endOffset

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
            "commit ${session.debugOneLine()} off=${composingStart}..${endOffset} coh=${coherence.name} " +
                "route=$acRoute ime=${context.packageName} beta=${BuildConfig.HEATMAP_LEARNING_BETA} " +
                "journal=${summary.journalUsedChars}/${summary.journalMaxChars} " +
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

    private fun recordWordMemoryAttempt(context: Context, session: WordSession_v5) {
        var enriched = session
        when (session.wordMemoryOutcome) {
            WordSessionOutcome_v1.KEPT_IN_FIELD -> {
                val resolved = when (session.inputMode) {
                    WordSessionInputMode_v1.SWIPE ->
                        HeatmapSwipeCorrectionChain_v2.onKeptWord(session)
                    WordSessionInputMode_v1.TAP ->
                        HeatmapSwipeCorrectionChain_v2.onKeptTapWord(
                            session.committedText, session.memorySequence,
                            session.fieldStartOffset, session.slotId,
                        )
                }
                if (resolved != null) {
                    HeatmapUserProfile_v1.onChainResolved(context, resolved)
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
            WordSessionOutcome_v1.SUPERSEDED_BY_LATER_EDIT,
            -> {
                if (session.inputMode == WordSessionInputMode_v1.SWIPE) {
                    HeatmapSwipeCorrectionChain_v2.onErasedAttempt(session)
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
        val composingStart = allocator.activeComposingStartOffset()
        val snap = helium314.keyboard.heatmap.swipe.HeatmapSwipeDecodeSnapshot_v3.peek()
        val linked = lastWordSession.get()?.slotId
        val session = HeatmapWordSessionBuilder_v4.fromDecodeSnapshot(
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
            fieldStartOffset = composingStart,
            fieldEndOffset = if (composingStart >= 0) composingStart + rejectedWord.length else -1,
            coherence = if (composingStart >= 0) WordSessionCoherence_v1.CLEAN else WordSessionCoherence_v1.SUSPECT,
        )
        recordWordMemoryAttempt(context, session)
        journal.appendWordAttempt(slot, rejectedWord, 0, WordSessionOutcome_v1.ERASED_BEFORE_COMMIT)
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
        recordErasedCopy(context, prior, WordSessionOutcome_v1.REVERTED_FROM_FIELD, hostPackage, false)
        helium314.keyboard.heatmap.swipe.HeatmapSwipeSlotRejectMemory_v7
            .recordFromLastSession(prior)
    }

    /** ai-note: user backspaced through a committed word in the text field; attribute by OFFSET. */
    @JvmStatic
    @JvmOverloads
    fun onFieldWordDeleted(
        context: Context,
        deletedWord: String?,
        cursorOffset: Int = -1,
        hostPackage: String?,
        editorInfo: EditorInfo? = null,
    ) {
        if (!HeatmapLearningGate_v1.shouldRecord(context, editorInfo)) return
        if (deletedWord.isNullOrEmpty()) return
        // ai-note: resolve the deleted word by its field position first (v6 only matched lastWordSession).
        val fieldHit = if (cursorOffset >= 0) fieldModel.wordCoveringEditAt(cursorOffset) else null
        val prior = when {
            fieldHit != null -> wordSessions.findBySlot(fieldHit.slotId) ?: lastWordSession.get()
            else -> lastWordSession.get()
        } ?: return
        // If the offset hit a word, trust it; otherwise keep v6's text-equality guard.
        if (fieldHit == null && !deletedWord.equals(prior.committedText, ignoreCase = true)) return

        val isEarlierWord = fieldHit != null && fieldModel.isEarlierWordOffset(fieldHit.startOffset)
        if (fieldHit != null) fieldModel.markSlotDeleted(fieldHit.slotId)

        recordErasedCopy(context, prior, WordSessionOutcome_v1.ERASED_FROM_FIELD, hostPackage, isEarlierWord)
        helium314.keyboard.heatmap.swipe.HeatmapSwipeSlotRejectMemory_v7.recordFromLastSession(prior)
    }

    private fun recordErasedCopy(
        context: Context,
        prior: WordSession_v5,
        outcome: WordSessionOutcome_v1,
        hostPackage: String?,
        mindChange: Boolean,
    ) {
        refreshFromSettings(context)
        val session = prior.copy(
            committedAtMs = System.currentTimeMillis(),
            wordMemoryOutcome = outcome,
            memorySequence = HeatmapWordMemoryLedger_v2.nextMemorySequence(),
            linkedSlotId = prior.slotId,
            hostPackage = hostPackage ?: prior.hostPackage,
            possibleMindChange = mindChange || prior.possibleMindChange,
        )
        recordWordMemoryAttempt(context, session)
        journal.appendWordAttempt(prior.slotId, prior.committedText, prior.separatorCharCount, outcome)
        Log.i(TAG_ALWAYS, "memory ${outcome.name.lowercase()} mindChange=$mindChange ${session.debugOneLine()}")
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
