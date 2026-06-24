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
class HeatmapLearningReset_v1Test {
    @Before
    fun clean() {
        val ctx = ApplicationProvider.getApplicationContext<android.content.Context>()
        HeatmapLearningDatabase_v1.resetForTests(ctx)
    }

    @Test
    fun wipeClearsDbButKeepsLearningEnabledPref() {
        val ctx = ApplicationProvider.getApplicationContext<android.content.Context>()
        val prefs = HeatmapCrossProcessPrefs_v2.readPrefs(ctx)
        prefs.edit()
            .putBoolean(HeatmapLearningSettings_v1.PREF_LEARNING_ENABLED, true)
            .putInt("heatmap_total_commits_ever", 99)
            .commit()
        val session = WordSession_v5(
            slotId = WordSlotId_v1(1),
            sessionGeneration = 1L,
            hostPackage = "com.test",
            inputMode = WordSessionInputMode_v1.TAP,
            commitType = WordSessionCommitType_v1.DECIDED,
            committedText = "hi",
            finalText = "hi",
            typedText = "hi",
            separatorCharCount = 1,
            committedAtMs = 1L,
        )
        HeatmapLearningStore_v1.recordWordCommit(ctx, session, null)
        assertEquals(1, HeatmapLearningStore_v1.readPersistedCommitCount(ctx))
        assertTrue(HeatmapLearningReset_v1.wipeAllTrainingData(ctx))
        assertEquals(0, HeatmapLearningStore_v1.readPersistedCommitCount(ctx))
        assertTrue(!prefs.contains("heatmap_total_commits_ever"))
        assertTrue(prefs.getBoolean(HeatmapLearningSettings_v1.PREF_LEARNING_ENABLED, false))
    }
}
