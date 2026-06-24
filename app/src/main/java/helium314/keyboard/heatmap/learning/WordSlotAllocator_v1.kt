// SPDX-License-Identifier: GPL-3.0-only
// ai-note: monotonic WordSlotId; one active composing slot until word commit
package helium314.keyboard.heatmap.learning

/**
 * Session-scoped allocator for [WordSlotId_v1].
 * [onComposingStarted] reserves the slot for the word in progress; [onWordCommitted] finalizes it.
 */
class WordSlotAllocator_v1 {
    private var sessionGeneration: Long = 0L
    private var nextId: Int = 1
    private var totalCommittedWords: Int = 0
    private var composingSlot: WordSlotId_v1? = null

    fun sessionGeneration(): Long = sessionGeneration

    fun totalCommittedWords(): Int = totalCommittedWords

    fun activeComposingSlot(): WordSlotId_v1? = composingSlot

    fun resetSession() {
        sessionGeneration += 1
        nextId = 1
        totalCommittedWords = 0
        composingSlot = null
    }

    /**
     * Call when tap or swipe begins composing a new word.
     * Re-entrant while the same word is composing returns the same slot.
     */
    fun onComposingStarted(): WordSlotId_v1 {
        composingSlot?.let { return it }
        val id = WordSlotId_v1(nextId)
        nextId += 1
        composingSlot = id
        return id
    }

    /**
     * Finalizes the active composing slot, or allocates one if commit happened without start hook.
     */
    fun onWordCommitted(): WordSlotId_v1 {
        val slot = composingSlot ?: WordSlotId_v1(nextId).also { nextId += 1 }
        composingSlot = null
        totalCommittedWords += 1
        return slot
    }
}
