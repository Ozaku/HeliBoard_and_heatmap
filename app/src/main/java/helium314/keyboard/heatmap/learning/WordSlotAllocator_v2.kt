// SPDX-License-Identifier: GPL-3.0-only

// ai-note: v2 — POSITION-AWARE slot allocator. v1 was a blind monotonic counter; nothing tied a
// slot to where the word lives in the field, so re-editing an earlier word minted a brand-new slot
// and the data for the original position was orphaned. v2 consults HeatmapFieldWordModel_v1: if the
// composing region starts inside an existing tracked word's [start,end) range, we REUSE that word's
// slot (this is a re-edit of an existing position); otherwise we allocate a fresh monotonic slot.
// AI EDIT MAP:
//   onComposingStarted(offset, fieldModel) -> reuse slot if offset hits an existing word, else new
//   onWordCommitted()                      -> finalize the active composing slot (or mint one)
//   reusedExistingSlot()                   -> true if the last onComposingStarted reused a position
// Behaviour with unknown offset (offset < 0) is identical to v1: always a new slot.
package helium314.keyboard.heatmap.learning

class WordSlotAllocator_v2 {
    private var sessionGeneration: Long = 0L
    private var nextId: Int = 1
    private var totalCommittedWords: Int = 0
    private var composingSlot: WordSlotId_v1? = null
    private var composingReusedExisting: Boolean = false
    private var composingStartOffset: Int = -1

    fun sessionGeneration(): Long = sessionGeneration

    fun totalCommittedWords(): Int = totalCommittedWords

    fun activeComposingSlot(): WordSlotId_v1? = composingSlot

    /** ai-note: true when the active composing slot was reused from an existing field position. */
    fun reusedExistingSlot(): Boolean = composingReusedExisting

    fun activeComposingStartOffset(): Int = composingStartOffset

    fun resetSession() {
        sessionGeneration += 1
        nextId = 1
        totalCommittedWords = 0
        composingSlot = null
        composingReusedExisting = false
        composingStartOffset = -1
    }

    /**
     * Begin composing a new word at absolute [composingStartOffset] (or -1 if unknown).
     * If that offset falls within a live tracked word, reuse its slot (re-edit); else allocate new.
     * Re-entrant while the same word is composing: returns the same slot.
     */
    fun onComposingStarted(
        composingStartOffset: Int,
        fieldModel: HeatmapFieldWordModel_v1?,
    ): WordSlotId_v1 {
        composingSlot?.let { return it }
        this.composingStartOffset = composingStartOffset
        val reused = if (composingStartOffset >= 0 && fieldModel != null) {
            fieldModel.wordAtOffset(composingStartOffset)?.slotId
        } else {
            null
        }
        return if (reused != null) {
            composingSlot = reused
            composingReusedExisting = true
            reused
        } else {
            val id = WordSlotId_v1(nextId)
            nextId += 1
            composingSlot = id
            composingReusedExisting = false
            id
        }
    }

    /** Back-compat overload (no field model) — always a fresh slot, like v1. */
    fun onComposingStarted(): WordSlotId_v1 = onComposingStarted(-1, null)

    /** Finalizes the active composing slot, or allocates one if commit happened without start hook. */
    fun onWordCommitted(): WordSlotId_v1 {
        val slot = composingSlot ?: WordSlotId_v1(nextId).also { nextId += 1 }
        val wasReused = composingReusedExisting
        composingSlot = null
        composingReusedExisting = false
        composingStartOffset = -1
        // ai-note: re-edits of an existing position do not increase the committed-word count.
        if (!wasReused) totalCommittedWords += 1
        return slot
    }
}
