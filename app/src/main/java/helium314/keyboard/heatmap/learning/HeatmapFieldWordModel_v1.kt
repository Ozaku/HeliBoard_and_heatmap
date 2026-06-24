// SPDX-License-Identifier: GPL-3.0-only

// ai-note: v1 — PRECISION BACKBONE for position-aware data collection.
// Holds an ordered list of committed words, each pinned to an absolute [startOffset,endOffset)
// character range in the text field. Kept in sync via commit events, onUpdateSelection deltas,
// and deletes. Everything downstream (slot reuse, correction-chain gating, finalText reconcile,
// mind-change / proofread flags) keys off wordAtOffset()/the offsets here.
// AI EDIT MAP:
//   FieldWord            -> one tracked committed word + its live offset range + outcome metadata
//   onCommit()           -> register a freshly committed word at [composingStart, +committedLen)
//   onTextDelta()        -> shift every word whose range starts at/after an edit offset by delta
//   onSelectionMoved()   -> remember the live cursor so re-edits can be attributed by position
//   wordAtOffset()       -> the word whose range contains an offset (slot reuse + edit attribution)
//   wordCoveringEditAt() -> nearest word affected by an insert/delete at an offset (incl. boundary)
//   markDeletedAt()/markReplacedAt() -> outcome bookkeeping when a tracked word is removed/changed
// Thread-safety: all IME hooks can arrive on different threads -> every method synchronizes on lock.
package helium314.keyboard.heatmap.learning

object HeatmapFieldWordModel_v1Defaults {
    const val UNKNOWN_OFFSET = -1
}

class HeatmapFieldWordModel_v1 {

    /** ai-note: live record of one committed word in the field. Offsets are absolute, mutable
     *  (shifted by onTextDelta as earlier text grows/shrinks). slotId pins it to its WordSession. */
    data class FieldWord(
        val slotId: WordSlotId_v1,
        var startOffset: Int,
        var endOffset: Int,
        var word: String,
        val inputMode: WordSessionInputMode_v1,
        val memorySequence: Int,
        val committedAtMs: Long,
        var alive: Boolean = true,
    ) {
        fun length(): Int = endOffset - startOffset
        fun contains(offset: Int): Boolean = offset in startOffset until endOffset
    }

    private val lock = Any()

    // ai-note: insertion order == field order; we keep it sorted by startOffset on every mutation.
    private val words = ArrayList<FieldWord>()

    @Volatile
    private var cursorOffset: Int = HeatmapFieldWordModel_v1Defaults.UNKNOWN_OFFSET

    @Volatile
    private var cursorKnown: Boolean = false

    fun clear() = synchronized(lock) {
        words.clear()
        cursorOffset = HeatmapFieldWordModel_v1Defaults.UNKNOWN_OFFSET
        cursorKnown = false
    }

    fun cursorOffset(): Int = cursorOffset

    fun isCursorKnown(): Boolean = cursorKnown

    /**
     * Register a committed word. [composingStart] is the absolute offset where the composing region
     * began; [committedLen] is the committed text length (without trailing separator). When the
     * offsets are unknown (composingStart < 0) the word is appended at the end of the field with
     * best-effort offsets derived from the running total so ordering is still preserved.
     */
    fun onCommit(
        word: String,
        composingStart: Int,
        committedLen: Int,
        slotId: WordSlotId_v1,
        inputMode: WordSessionInputMode_v1,
        memorySequence: Int,
        committedAtMs: Long,
    ): FieldWord = synchronized(lock) {
        val start = if (composingStart >= 0) composingStart else endOfFieldLocked()
        val end = start + committedLen.coerceAtLeast(0)
        // ai-note: re-commit over an existing range (re-edit of same position) reuses that slot's row.
        val existing = words.firstOrNull { it.alive && it.slotId == slotId }
        if (existing != null) {
            existing.startOffset = start
            existing.endOffset = end
            existing.word = word
            existing.alive = true
            sortLocked()
            return existing
        }
        val fw = FieldWord(
            slotId = slotId,
            startOffset = start,
            endOffset = end,
            word = word,
            inputMode = inputMode,
            memorySequence = memorySequence,
            committedAtMs = committedAtMs,
        )
        words.add(fw)
        sortLocked()
        fw
    }

    /** Shift all words that begin at/after [atOffset] by [delta] (delta may be negative). */
    fun onTextDelta(atOffset: Int, delta: Int) = synchronized(lock) {
        if (delta == 0) return
        for (w in words) {
            if (!w.alive) continue
            if (w.startOffset >= atOffset) {
                w.startOffset += delta
                w.endOffset += delta
            } else if (w.endOffset > atOffset) {
                // edit landed inside this word's range -> grow/shrink only its end.
                w.endOffset = (w.endOffset + delta).coerceAtLeast(w.startOffset)
            }
        }
    }

    /** Convenience alias used by callers thinking in "shift everything after a point" terms. */
    fun shiftAfter(offset: Int, delta: Int) = onTextDelta(offset, delta)

    fun onSelectionMoved(newStart: Int, newEnd: Int) = synchronized(lock) {
        cursorOffset = newStart
        cursorKnown = newStart >= 0
    }

    fun setCursorUnknown() = synchronized(lock) {
        cursorKnown = false
        cursorOffset = HeatmapFieldWordModel_v1Defaults.UNKNOWN_OFFSET
    }

    /** The live word whose [start,end) range contains [offset], or null. */
    fun wordAtOffset(offset: Int): FieldWord? = synchronized(lock) {
        if (offset < 0) return null
        words.firstOrNull { it.alive && it.contains(offset) }
    }

    /**
     * The word affected by an insert/delete acting AT [offset]. Unlike [wordAtOffset] this also
     * matches when the edit sits on a word's trailing boundary (offset == endOffset), which is the
     * common case for backspacing the last char of a word or appending to it.
     */
    fun wordCoveringEditAt(offset: Int): FieldWord? = synchronized(lock) {
        if (offset < 0) return null
        words.firstOrNull { it.alive && offset in it.startOffset..it.endOffset }
    }

    /** True if [offset] lands strictly before the last tracked word's end (i.e. an earlier-word edit). */
    fun isEarlierWordOffset(offset: Int): Boolean = synchronized(lock) {
        if (offset < 0) return false
        val last = words.lastOrNull { it.alive } ?: return false
        offset < last.startOffset
    }

    fun lastAliveWord(): FieldWord? = synchronized(lock) { words.lastOrNull { it.alive } }

    fun findBySlot(slotId: WordSlotId_v1): FieldWord? =
        synchronized(lock) { words.firstOrNull { it.slotId == slotId } }

    /** All currently alive words after [offset] (exclusive), field order. */
    fun aliveWordsAfter(offset: Int): List<FieldWord> = synchronized(lock) {
        words.filter { it.alive && it.startOffset >= offset }
    }

    fun aliveCount(): Int = synchronized(lock) { words.count { it.alive } }

    fun snapshotOldestFirst(): List<FieldWord> = synchronized(lock) { words.map { it.copy() } }

    /** Mark a tracked word as removed from the field (does not shift others; caller does via onTextDelta). */
    fun markDeletedAt(offset: Int): FieldWord? = synchronized(lock) {
        val hit = words.firstOrNull { it.alive && offset in it.startOffset..it.endOffset } ?: return null
        hit.alive = false
        hit
    }

    fun markSlotDeleted(slotId: WordSlotId_v1): FieldWord? = synchronized(lock) {
        val hit = words.firstOrNull { it.alive && it.slotId == slotId } ?: return null
        hit.alive = false
        hit
    }

    private fun endOfFieldLocked(): Int {
        val last = words.lastOrNull { it.alive } ?: return 0
        return last.endOffset + 1
    }

    private fun sortLocked() {
        words.sortBy { it.startOffset }
    }
}
