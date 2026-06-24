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
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class HeatmapLearningStore_v1Test {
    @Before
    fun wipeDb() {
        val ctx = ApplicationProvider.getApplicationContext<android.content.Context>()
        HeatmapLearningDatabase_v1.resetForTests(ctx)
    }

    @Test
    fun commitsPersistAndBuildWordPair() {
        val ctx = ApplicationProvider.getApplicationContext<android.content.Context>()
        val first = session(1, "hello")
        val second = session(2, "world")
        HeatmapLearningStore_v1.recordWordCommit(ctx, first, null)
        HeatmapLearningStore_v1.recordWordCommit(ctx, second, "hello")
        val snap = HeatmapLearningStore_v1.readSnapshot(ctx)
        assertEquals(2, snap.commitCount)
        assertTrue(snap.keyStatRows >= 1)
        assertEquals(1, snap.wordPairRows)
        assertTrue(File(snap.dbPath).exists())
    }

    private fun session(slot: Int, word: String) = WordSession_v5(
        slotId = WordSlotId_v1(slot),
        sessionGeneration = 1L,
        hostPackage = "com.test",
        inputMode = WordSessionInputMode_v1.TAP,
        commitType = WordSessionCommitType_v1.DECIDED,
        committedText = word,
        finalText = word,
        typedText = word,
        separatorCharCount = 1,
        committedAtMs = System.currentTimeMillis(),
    )
}
