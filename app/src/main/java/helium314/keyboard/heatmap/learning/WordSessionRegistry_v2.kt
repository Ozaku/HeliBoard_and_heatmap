// SPDX-License-Identifier: GPL-3.0-only

package helium314.keyboard.heatmap.learning



class WordSessionRegistry_v2(

    private val maxSessions: Int = 64,

) {

    private val sessions = ArrayDeque<WordSession_v2>()

    /** Slots committed but not yet reconciled with host field text (step 1.12 stub). */
    private val pendingFinalTextSync = LinkedHashSet<WordSlotId_v1>()



    fun record(session: WordSession_v2) {

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



    fun lastOrNull(): WordSession_v2? = sessions.lastOrNull()



    fun findBySlot(slotId: WordSlotId_v1): WordSession_v2? =
        sessions.lastOrNull { it.slotId == slotId }

    /** ai-note: 1.13 export — oldest-first for readable JSON */
    fun copySessionsOldestFirst(): List<WordSession_v2> = sessions.toList()

    fun pendingFinalTextSyncCount(): Int = pendingFinalTextSync.size

    /** Step 1.12 stub — keeps pending set; 1.13+ will clear slots when finalText synced */
    fun markStableAtFlush(): Int = pendingFinalTextSync.size

    fun markEditSuspected() { /* pending set unchanged; selection hook counts as suspicion */ }
}

