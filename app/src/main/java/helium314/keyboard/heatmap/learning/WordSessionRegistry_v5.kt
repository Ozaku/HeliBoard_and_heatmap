// SPDX-License-Identifier: GPL-3.0-only

// ai-note: v5 registry - 300 entries aligned with word memory ledger v2

package helium314.keyboard.heatmap.learning

class WordSessionRegistry_v5(

    private val maxSessions: Int = HeatmapWordMemoryLedger_v2.MAX_WORD_ENTRIES,

) {

    private val sessions = ArrayDeque<WordSession_v5>()

    private val pendingFinalTextSync = LinkedHashSet<WordSlotId_v1>()

    fun record(session: WordSession_v5) {

        sessions.addLast(session)

        pendingFinalTextSync.add(session.slotId)

        while (sessions.size > maxSessions) {

            pendingFinalTextSync.remove(sessions.removeFirst().slotId)

        }

    }

    fun clear() {

        sessions.clear()

        pendingFinalTextSync.clear()

    }

    fun size(): Int = sessions.size

    fun lastOrNull(): WordSession_v5? = sessions.lastOrNull()

    fun findBySlot(slotId: WordSlotId_v1): WordSession_v5? =
        sessions.lastOrNull { it.slotId == slotId }

    /** ai-note: replace the most-recent session for [slotId] via [transform]; true if one matched. */
    fun updateLastBySlot(slotId: WordSlotId_v1, transform: (WordSession_v5) -> WordSession_v5): Boolean {
        for (i in sessions.indices.reversed()) {
            if (sessions[i].slotId == slotId) {
                sessions[i] = transform(sessions[i])
                return true
            }
        }
        return false
    }

    /** ai-note: reconcile the word now living at a slot's position; true if finalText changed. */
    fun updateFinalText(slotId: WordSlotId_v1, finalText: String): Boolean {
        var changed = false
        updateLastBySlot(slotId) { s ->
            if (s.finalText == finalText) s else { changed = true; s.copy(finalText = finalText) }
        }
        return changed
    }

    fun copySessionsOldestFirst(): List<WordSession_v5> = sessions.toList()

    fun pendingFinalTextSyncCount(): Int = pendingFinalTextSync.size

    fun markStableAtFlush(): Int = pendingFinalTextSync.size

    fun markEditSuspected() { /* pending set unchanged */ }
}