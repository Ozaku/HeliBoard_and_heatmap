// SPDX-License-Identifier: GPL-3.0-only

// ai-note: validates the position-aware data layer: coherence-gated correction chains (way->did
// splits to orphans; care/come->create stays one chain), the field offset model, position-aware slot
// reuse, finalText reconciliation, and proofread-jump classification.
package helium314.keyboard.heatmap.learning

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class PositionAwareWordData_v1Test {

    @Before
    fun clean() {
        HeatmapSwipeCorrectionChain_v2.clearAll()
    }

    private fun swipeAttempt(
        slot: Int,
        text: String,
        visitOrder: String?,
        offset: Int,
        outcome: WordSessionOutcome_v1,
        memSeq: Int,
    ) = WordSession_v5(
        slotId = WordSlotId_v1(slot),
        sessionGeneration = 1L,
        hostPackage = "com.test",
        inputMode = WordSessionInputMode_v1.SWIPE,
        commitType = WordSessionCommitType_v1.DECIDED,
        committedText = text,
        finalText = text,
        typedText = text,
        separatorCharCount = 1,
        committedAtMs = 1000L,
        wordMemoryOutcome = outcome,
        memorySequence = memSeq,
        swipeVisitOrder = visitOrder,
        fieldStartOffset = offset,
    )

    @Test
    fun wayRetriesAbandonedForDidSplitToOrphans() {
        // 6 abandoned "way" swipes (raw path ~ w,a,y) then a kept "did" at the same cleared position.
        for (i in 1..6) {
            HeatmapSwipeCorrectionChain_v2.onErasedAttempt(
                swipeAttempt(1, "way", "way", 0, WordSessionOutcome_v1.ERASED_BEFORE_COMMIT, i),
            )
        }
        val kept = swipeAttempt(2, "did", null, 0, WordSessionOutcome_v1.KEPT_IN_FIELD, 7)
        val chain = HeatmapSwipeCorrectionChain_v2.onKeptWord(kept)
        assertNull("did is not a plausible decode of a 'way' path -> no chain", chain)
        assertEquals(6, HeatmapSwipeCorrectionChain_v2.copyOrphansOldestFirst().size)
        assertTrue(HeatmapSwipeCorrectionChain_v2.copyResolvedOldestFirst().isEmpty())
    }

    @Test
    fun careComeRetriesForCreateStayOneCleanChain() {
        // Two failed swipes whose RAW swept path is create-like, decoded to care/come, then kept create.
        HeatmapSwipeCorrectionChain_v2.onErasedAttempt(
            swipeAttempt(1, "care", "create", 0, WordSessionOutcome_v1.ERASED_BEFORE_COMMIT, 1),
        )
        HeatmapSwipeCorrectionChain_v2.onErasedAttempt(
            swipeAttempt(1, "come", "create", 0, WordSessionOutcome_v1.ERASED_BEFORE_COMMIT, 2),
        )
        val kept = swipeAttempt(1, "create", null, 0, WordSessionOutcome_v1.KEPT_IN_FIELD, 3)
        val chain = HeatmapSwipeCorrectionChain_v2.onKeptWord(kept)
        assertNotNull(chain)
        assertEquals(3, chain!!.attempts.size) // 2 retries + kept
        assertEquals("create", chain.finalWord)
        assertTrue(HeatmapSwipeCorrectionChain_v2.copyOrphansOldestFirst().isEmpty())
    }

    @Test
    fun targetCompatibilityGate() {
        assertTrue(HeatmapSwipeCorrectionChain_v2.isTargetCompatible("create", "create", null))
        assertFalse(HeatmapSwipeCorrectionChain_v2.isTargetCompatible("did", "way", "way"))
    }

    @Test
    fun fieldModelTracksOffsetsAndShifts() {
        val fm = HeatmapFieldWordModel_v1()
        fm.onCommit("hello", 0, 5, WordSlotId_v1(1), WordSessionInputMode_v1.TAP, 1, 0L)
        fm.onCommit("world", 6, 5, WordSlotId_v1(2), WordSessionInputMode_v1.TAP, 2, 0L)
        assertEquals(WordSlotId_v1(1), fm.wordAtOffset(3)?.slotId)
        assertEquals(WordSlotId_v1(2), fm.wordAtOffset(8)?.slotId)
        // Insert 3 chars at the very start -> both words shift right by 3.
        fm.onTextDelta(0, 3)
        assertEquals(WordSlotId_v1(1), fm.wordAtOffset(4)?.slotId)
        assertEquals(WordSlotId_v1(2), fm.wordAtOffset(11)?.slotId)
    }

    @Test
    fun slotAllocatorReusesExistingPositionElseNew() {
        // Drive slots through the allocator, mirroring production (allocator is the sole slot source).
        val fm = HeatmapFieldWordModel_v1()
        val alloc = WordSlotAllocator_v2()
        val first = alloc.onComposingStarted(0, fm)
        alloc.onWordCommitted()
        fm.onCommit("hello", 0, 5, first, WordSessionInputMode_v1.TAP, 1, 0L)
        // Re-edit inside "hello" -> reuse its slot.
        val reused = alloc.onComposingStarted(2, fm)
        assertEquals(first, reused)
        assertTrue(alloc.reusedExistingSlot())
        alloc.onWordCommitted()
        // Compose in empty space -> a brand new slot.
        val fresh = alloc.onComposingStarted(40, fm)
        assertFalse(alloc.reusedExistingSlot())
        assertTrue(fresh.value != first.value)
    }

    @Test
    fun fieldProbeReadsWordAtOffset() {
        val probe = FieldTextProbe_v2.WindowProbe(
            cursorOffset = 11, windowStart = 0, before = "hello world", after = "",
        )
        assertEquals("hello", probe.wordCoveringOffset(0))
        assertEquals("world", probe.wordCoveringOffset(7))
    }

    @Test
    fun reconcileFinalTextUpdatesSlotFromField() {
        val fm = HeatmapFieldWordModel_v1()
        fm.onCommit("teh", 0, 3, WordSlotId_v1(1), WordSessionInputMode_v1.TAP, 1, 0L)
        val reg = WordSessionRegistry_v5()
        reg.record(
            WordSession_v5(
                slotId = WordSlotId_v1(1),
                sessionGeneration = 1L,
                hostPackage = "com.test",
                inputMode = WordSessionInputMode_v1.TAP,
                commitType = WordSessionCommitType_v1.DECIDED,
                committedText = "teh",
                finalText = "teh",
                typedText = "teh",
                separatorCharCount = 1,
                committedAtMs = 0L,
                fieldStartOffset = 0,
                fieldEndOffset = 3,
            ),
        )
        val probe = FieldTextProbe_v2.WindowProbe(3, 0, "the", "")
        val result = InWindowEditDetector_v2.reconcileFinalText(fm, probe, reg)
        assertEquals(1, result.updated)
        assertEquals("the", reg.findBySlot(WordSlotId_v1(1))?.finalText)
    }

    @Test
    fun proofreadJumpClassification() {
        assertTrue(InWindowEditDetector_v2.classifyCursorJump(10, 30))
        assertFalse(InWindowEditDetector_v2.classifyCursorJump(10, 13))
        assertFalse(InWindowEditDetector_v2.classifyCursorJump(-1, 30))
    }
}
