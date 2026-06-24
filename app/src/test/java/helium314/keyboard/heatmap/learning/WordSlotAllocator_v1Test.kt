// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.heatmap.learning

import org.junit.Assert.assertEquals
import org.junit.Test

class WordSlotAllocator_v1Test {
    @Test
    fun assignsMonotonicIds() {
        val allocator = WordSlotAllocator_v1()
        allocator.onComposingStarted()
        assertEquals(WordSlotId_v1(1), allocator.onWordCommitted())
        allocator.onComposingStarted()
        assertEquals(WordSlotId_v1(2), allocator.onWordCommitted())
        assertEquals(2, allocator.totalCommittedWords())
    }

    @Test
    fun composingReusesSlotUntilCommit() {
        val allocator = WordSlotAllocator_v1()
        val a = allocator.onComposingStarted()
        val b = allocator.onComposingStarted()
        assertEquals(a, b)
        assertEquals(a, allocator.onWordCommitted())
        assertEquals(null, allocator.activeComposingSlot())
    }

    @Test
    fun resetSessionRestartsIds() {
        val allocator = WordSlotAllocator_v1()
        allocator.onWordCommitted()
        val genBefore = allocator.sessionGeneration()
        allocator.resetSession()
        assertEquals(genBefore + 1, allocator.sessionGeneration())
        assertEquals(WordSlotId_v1(1), allocator.onComposingStarted())
        assertEquals(WordSlotId_v1(1), allocator.onWordCommitted())
        assertEquals(1, allocator.totalCommittedWords())
    }
}
