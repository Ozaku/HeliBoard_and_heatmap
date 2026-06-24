// SPDX-License-Identifier: GPL-3.0-only

// ai-note: v4 registry — 300 entries aligned with word memory ledger

package helium314.keyboard.heatmap.learning

class WordSessionRegistry_v4(

    private val maxSessions: Int = HeatmapWordMemoryLedger_v1.MAX_WORD_ENTRIES,

) {

    private val sessions = ArrayDeque<WordSession_v4>()

    private val pendingFinalTextSync = LinkedHashSet<WordSlotId_v1>()

    fun record(session: WordSession_v4) {

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

    fun lastOrNull(): WordSession_v4? = sessions.lastOrNull()

    fun findBySlot(slotId: WordSlotId_v1): WordSession_v4? =
        sessions.lastOrNull { it.slotId == slotId }

    fun copySessionsOldestFirst(): List<WordSession_v4> = sessions.toList()

    fun pendingFinalTextSyncCount(): Int = pendingFinalTextSync.size

    fun markStableAtFlush(): Int = pendingFinalTextSync.size

    fun markEditSuspected() { /* pending set unchanged */ }
}
