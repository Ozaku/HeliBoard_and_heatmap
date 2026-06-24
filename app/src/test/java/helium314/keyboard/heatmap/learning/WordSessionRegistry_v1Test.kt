// SPDX-License-Identifier: GPL-3.0-only

package helium314.keyboard.heatmap.learning



import org.junit.Assert.assertEquals

import org.junit.Assert.assertNull

import org.junit.Test



class WordSessionRegistry_v2Test {

    @Test

    fun recordsAndFindsBySlot() {

        val registry = WordSessionRegistry_v2(maxSessions = 2)

        val slot1 = WordSlotId_v1(1)

        val slot2 = WordSlotId_v1(2)

        val s1 = session(slot1, "hello")

        val s2 = session(slot2, "world")

        registry.record(s1)

        registry.record(s2)

        assertEquals(2, registry.size())

        assertEquals(s2, registry.lastOrNull())

        assertEquals(s2, registry.findBySlot(slot2))

        assertEquals(s1, registry.findBySlot(slot1))

    }



    @Test

    fun trimsOldestWhenOverMax() {

        val registry = WordSessionRegistry_v2(maxSessions = 2)

        registry.record(session(WordSlotId_v1(1), "a"))

        registry.record(session(WordSlotId_v1(2), "b"))

        registry.record(session(WordSlotId_v1(3), "c"))

        assertEquals(2, registry.size())

        assertNull(registry.findBySlot(WordSlotId_v1(1)))

        assertEquals("c", registry.lastOrNull()?.committedText)

    }



    private fun session(slot: WordSlotId_v1, word: String) = WordSession_v2(

        slotId = slot,

        sessionGeneration = 1L,

        hostPackage = "com.test",

        inputMode = WordSessionInputMode_v1.TAP,

        commitType = WordSessionCommitType_v1.DECIDED,

        committedText = word,

        finalText = word,

        typedText = word,

        separatorCharCount = 1,

        committedAtMs = 0L,

    )

}

