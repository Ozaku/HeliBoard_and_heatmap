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
class HeatmapLearningReset_v2Test {
    @Before
    fun clean() {
        val ctx = ApplicationProvider.getApplicationContext<android.content.Context>()
        HeatmapLearningDatabase_v1.resetForTests(ctx)
    }

    @Test
    fun wipeGeometryKeepsCommits() {
        val ctx = ApplicationProvider.getApplicationContext<android.content.Context>()
        recordCommit(ctx, "hi")
        insertBoundsRow(ctx)
        assertEquals(1, HeatmapLearningStore_v1.readPersistedCommitCount(ctx))
        assertTrue(HeatmapLearningReset_v2.wipeGeometryData(ctx))
        assertEquals(1, HeatmapLearningStore_v1.readPersistedCommitCount(ctx))
        assertEquals(0, countTable(ctx, HeatmapLearningDatabase_v1.TABLE_KEY_BOUNDS))
    }

    @Test
    fun wipeSwipePathKeepsCommitsAndBounds() {
        val ctx = ApplicationProvider.getApplicationContext<android.content.Context>()
        recordCommit(ctx, "swipe")
        insertBoundsRow(ctx)
        insertPathBucket(ctx)
        assertTrue(HeatmapLearningReset_v2.wipeSwipePathBiases(ctx))
        assertEquals(1, HeatmapLearningStore_v1.readPersistedCommitCount(ctx))
        assertEquals(1, countTable(ctx, HeatmapLearningDatabase_v1.TABLE_KEY_BOUNDS))
        assertEquals(0, countTable(ctx, HeatmapLearningDatabase_v1.TABLE_PATH_BUCKET))
    }

    @Test
    fun wipeTypoWeightsClearsCorrectionTablesKeepsCommits() {
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
            committedAtMs = 1L,
        )
        HeatmapLearningStore_v1.recordWordCommit(ctx, session, null)
        assertEquals(1, HeatmapLearningStore_v1.readSnapshot(ctx).wordCorrectionRows)
        assertTrue(HeatmapLearningReset_v2.wipeTypoWeightsStub(ctx))
        val snap = HeatmapLearningStore_v1.readSnapshot(ctx)
        assertEquals(1, snap.commitCount)
        assertEquals(0, snap.wordCorrectionRows)
        assertEquals(0, snap.letterConfusionRows)
    }

    private fun recordCommit(ctx: android.content.Context, word: String) {
        val session = WordSession_v5(
            slotId = WordSlotId_v1(1),
            sessionGeneration = 1L,
            hostPackage = "com.test",
            inputMode = WordSessionInputMode_v1.TAP,
            commitType = WordSessionCommitType_v1.DECIDED,
            committedText = word,
            finalText = word,
            typedText = word,
            separatorCharCount = 1,
            committedAtMs = 1L,
        )
        HeatmapLearningStore_v1.recordWordCommit(ctx, session, null)
    }

    private fun insertBoundsRow(ctx: android.content.Context) {
        val db = HeatmapLearningDatabase_v1.getInstance(ctx).writableDatabase
        db.execSQL(
            """
            INSERT INTO ${HeatmapLearningDatabase_v1.TABLE_KEY_BOUNDS}
              (${HeatmapLearningDatabase_v1.COL_LOCALE}, ${HeatmapLearningDatabase_v1.COL_LAYOUT_HASH},
               ${HeatmapLearningDatabase_v1.COL_KEY_LABEL}, ${HeatmapLearningDatabase_v1.COL_KEY_CODE},
               ${HeatmapLearningDatabase_v1.COL_LEFT_PX}, ${HeatmapLearningDatabase_v1.COL_TOP_PX},
               ${HeatmapLearningDatabase_v1.COL_RIGHT_PX}, ${HeatmapLearningDatabase_v1.COL_BOTTOM_PX},
               ${HeatmapLearningDatabase_v1.COL_CENTER_X}, ${HeatmapLearningDatabase_v1.COL_CENTER_Y})
            VALUES ('en-US', 'lh_test', 'a', 97, 0, 0, 10, 10, 5, 5)
            """.trimIndent(),
        )
    }

    private fun insertPathBucket(ctx: android.content.Context) {
        val db = HeatmapLearningDatabase_v1.getInstance(ctx).writableDatabase
        db.execSQL(
            """
            INSERT INTO ${HeatmapLearningDatabase_v1.TABLE_PATH_BUCKET}
              (${HeatmapLearningDatabase_v1.COL_LOCALE}, ${HeatmapLearningDatabase_v1.COL_LAYOUT_HASH},
               ${HeatmapLearningDatabase_v1.COL_INPUT_MODE}, ${HeatmapLearningDatabase_v1.COL_SIGNATURE_HASH},
               ${HeatmapLearningDatabase_v1.COL_SEEN_COUNT}, ${HeatmapLearningDatabase_v1.COL_POLYLINE_BLOB})
            VALUES ('en-US', 'lh_test', 'SWIPE', 'ps_deadbeef', 1, X'00')
            """.trimIndent(),
        )
    }

    private fun countTable(ctx: android.content.Context, table: String): Int =
        HeatmapLearningDatabase_v1.getInstance(ctx).readableDatabase
            .rawQuery("SELECT COUNT(*) FROM $table", null)
            .use { c -> if (c.moveToFirst()) c.getInt(0) else 0 }
}
