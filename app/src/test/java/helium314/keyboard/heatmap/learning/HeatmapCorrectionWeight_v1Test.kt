// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.heatmap.learning

import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class HeatmapCorrectionWeight_v1Test {
    @Before
    fun clean() {
        val ctx = ApplicationProvider.getApplicationContext<android.content.Context>()
        HeatmapLearningDatabase_v1.resetForTests(ctx)
    }

    @Test
    fun collectLetterDiffsFindsMiddleMismatch() {
        val diffs = HeatmapCorrectionWeight_v1.collectLetterDiffs("clinb", "climb")
        assertEquals(1, diffs.size)
        assertEquals("n", diffs[0].fromLabel)
        assertEquals("m", diffs[0].toLabel)
        assertEquals(HeatmapLetterPositionBand_v1.MIDDLE, diffs[0].band)
    }

    @Test
    fun positionBandUsesFirstMiddleLast() {
        assertEquals(HeatmapLetterPositionBand_v1.FIRST, HeatmapCorrectionWeight_v1.positionBand(0, 5))
        assertEquals(HeatmapLetterPositionBand_v1.LAST, HeatmapCorrectionWeight_v1.positionBand(4, 5))
        assertEquals(HeatmapLetterPositionBand_v1.MIDDLE, HeatmapCorrectionWeight_v1.positionBand(2, 5))
    }

    @Test
    fun applyOnCommitPersistsWordAndLetterRows() {
        val ctx = ApplicationProvider.getApplicationContext<android.content.Context>()
        val session = WordSession_v5(
            slotId = WordSlotId_v1(1),
            sessionGeneration = 1L,
            hostPackage = "com.test",
            inputMode = WordSessionInputMode_v1.TAP,
            commitType = WordSessionCommitType_v1.DECIDED,
            committedText = "climb",
            finalText = "climb",
            typedText = "clinb",
            separatorCharCount = 1,
            committedAtMs = 1000L,
        )
        HeatmapLearningStore_v1.recordWordCommit(ctx, session, null)
        val snap = HeatmapLearningStore_v1.readSnapshot(ctx)
        assertEquals(1, snap.wordCorrectionRows)
        assertEquals(1, snap.letterConfusionRows)
    }

    @Test
    fun matchingTypedCommittedSkipsWeightRows() {
        val ctx = ApplicationProvider.getApplicationContext<android.content.Context>()
        val session = WordSession_v5(
            slotId = WordSlotId_v1(1),
            sessionGeneration = 1L,
            hostPackage = "com.test",
            inputMode = WordSessionInputMode_v1.TAP,
            commitType = WordSessionCommitType_v1.DECIDED,
            committedText = "hello",
            finalText = "hello",
            typedText = "hello",
            separatorCharCount = 1,
            committedAtMs = 1000L,
        )
        HeatmapLearningStore_v1.recordWordCommit(ctx, session, null)
        val snap = HeatmapLearningStore_v1.readSnapshot(ctx)
        assertEquals(0, snap.wordCorrectionRows)
        assertEquals(0, snap.letterConfusionRows)
    }

    @Test
    fun decayReducesStaleWeight() {
        val dayMs = 24L * 60L * 60L * 1000L
        val now = 40L * dayMs
        val stale = now - (31L * dayMs)
        val decayed = HeatmapCorrectionWeight_v1.applyDecay(100, stale, now)
        assertEquals(90, decayed)
    }

    @Test
    fun middleBandIncrementsMoreThanFirst() {
        assertTrue(
            HeatmapCorrectionWeight_v1.bandIncrement(HeatmapLetterPositionBand_v1.MIDDLE) >
                HeatmapCorrectionWeight_v1.bandIncrement(HeatmapLetterPositionBand_v1.FIRST),
        )
    }
}
