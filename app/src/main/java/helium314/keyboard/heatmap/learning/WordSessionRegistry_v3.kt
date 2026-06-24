// SPDX-License-Identifier: GPL-3.0-only

// ai-note: Phase1 step 1.3 — registry for v3 sessions with swipe trace export

package helium314.keyboard.heatmap.learning

class WordSessionRegistry_v3(

    private val maxSessions: Int = 64,

) {

    private val sessions = ArrayDeque<WordSession_v3>()

    private val pendingFinalTextSync = LinkedHashSet<WordSlotId_v1>()

    fun record(session: WordSession_v3) {

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

    fun lastOrNull(): WordSession_v3? = sessions.lastOrNull()

    fun findBySlot(slotId: WordSlotId_v1): WordSession_v3? =
        sessions.lastOrNull { it.slotId == slotId }

    fun copySessionsOldestFirst(): List<WordSession_v3> = sessions.toList()

    fun pendingFinalTextSyncCount(): Int = pendingFinalTextSync.size

    fun markStableAtFlush(): Int = pendingFinalTextSync.size

    fun markEditSuspected() { /* pending set unchanged */ }
}
