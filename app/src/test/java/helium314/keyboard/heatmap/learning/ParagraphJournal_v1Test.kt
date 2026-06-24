// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.heatmap.learning

import org.junit.Assert.assertEquals
import org.junit.Test

class ParagraphJournal_v1Test {
    @Test
    fun trimsOldestWhenOverBudget() {
        val journal = ParagraphJournal_v1(initialMaxChars = 20)
        journal.appendCommittedWord(WordSlotId_v1(1), "hello", 1) // 6
        journal.appendCommittedWord(WordSlotId_v1(2), "world", 1) // 6 -> 12
        journal.appendCommittedWord(WordSlotId_v1(3), "longword", 1) // 9 -> 21, drops hello
        assertEquals(15, journal.usedChars())
        assertEquals(2, journal.entryCount())
        val newest = journal.entriesNewestFirst().first()
        assertEquals(WordSlotId_v1(3), newest.slotId)
    }

    @Test
    fun setMaxCharsTrimsImmediately() {
        val journal = ParagraphJournal_v1(initialMaxChars = 100)
        journal.appendCommittedWord(WordSlotId_v1(1), "abcdefghij", 0)
        journal.setMaxChars(5)
        assertEquals(0, journal.entryCount())
        assertEquals(0, journal.usedChars())
    }
}
