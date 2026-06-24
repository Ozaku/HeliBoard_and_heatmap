// SPDX-License-Identifier: GPL-3.0-only

// ai-note: in-IME-process ring of WordSession_v1 for paragraph window / export (step 1.8)

package helium314.keyboard.heatmap.learning



/**

 * Holds recent [WordSession_v1] entries in the IME process only.

 */

class WordSessionRegistry_v1(

    private val maxSessions: Int = 64,

) {

    private val sessions = ArrayDeque<WordSession_v1>()



    fun record(session: WordSession_v1) {

        sessions.addLast(session)

        while (sessions.size > maxSessions) {

            sessions.removeFirst()

        }

    }



    fun clear() {

        sessions.clear()

    }



    fun size(): Int = sessions.size



    fun newestFirst(): List<WordSession_v1> = sessions.reversed()



    fun lastOrNull(): WordSession_v1? = sessions.lastOrNull()



    fun findBySlot(slotId: WordSlotId_v1): WordSession_v1? =

        sessions.lastOrNull { it.slotId == slotId }

}

