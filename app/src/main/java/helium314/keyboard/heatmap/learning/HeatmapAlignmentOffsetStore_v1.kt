// SPDX-License-Identifier: GPL-3.0-only

// ai-note: Block 2 step 10 — running finger-vs-key-center offsets per letter key (swipe)

package helium314.keyboard.heatmap.learning

import android.database.sqlite.SQLiteDatabase

object HeatmapAlignmentOffsetStore_v1 {

    fun bumpFromResampledPoints(
        db: SQLiteDatabase,
        locale: String,
        layoutHash: String,
        inputMode: String,
        layout: HeatmapCoordinateMap_v1.Snapshot,
        points: List<HeatmapPathSignature_v1.Result.Point>,
    ) {
        for (p in points) {
            val key = layout.keyAt(p.x, p.y) ?: continue
            val dx = p.x - key.centerX
            val dy = p.y - key.centerY
            bumpOffset(db, locale, layoutHash, inputMode, key.storageLabel, dx, dy)
        }
    }

    private fun bumpOffset(
        db: SQLiteDatabase,
        locale: String,
        layoutHash: String,
        inputMode: String,
        keyLabel: String,
        dx: Int,
        dy: Int,
    ) {
        db.execSQL(
            """
            INSERT INTO ${HeatmapLearningDatabase_v1.TABLE_ALIGNMENT_OFFSET}
              (${HeatmapLearningDatabase_v1.COL_LOCALE}, ${HeatmapLearningDatabase_v1.COL_LAYOUT_HASH},
               ${HeatmapLearningDatabase_v1.COL_INPUT_MODE}, ${HeatmapLearningDatabase_v1.COL_KEY_LABEL},
               ${HeatmapLearningDatabase_v1.COL_SAMPLE_COUNT},
               ${HeatmapLearningDatabase_v1.COL_SUM_DX}, ${HeatmapLearningDatabase_v1.COL_SUM_DY})
            VALUES (?, ?, ?, ?, 1, ?, ?)
            ON CONFLICT(${HeatmapLearningDatabase_v1.COL_LOCALE}, ${HeatmapLearningDatabase_v1.COL_LAYOUT_HASH},
              ${HeatmapLearningDatabase_v1.COL_INPUT_MODE}, ${HeatmapLearningDatabase_v1.COL_KEY_LABEL})
            DO UPDATE SET
              ${HeatmapLearningDatabase_v1.COL_SAMPLE_COUNT} =
                ${HeatmapLearningDatabase_v1.COL_SAMPLE_COUNT} + 1,
              ${HeatmapLearningDatabase_v1.COL_SUM_DX} =
                ${HeatmapLearningDatabase_v1.COL_SUM_DX} + excluded.${HeatmapLearningDatabase_v1.COL_SUM_DX},
              ${HeatmapLearningDatabase_v1.COL_SUM_DY} =
                ${HeatmapLearningDatabase_v1.COL_SUM_DY} + excluded.${HeatmapLearningDatabase_v1.COL_SUM_DY}
            """.trimIndent(),
            arrayOf<Any>(locale, layoutHash, inputMode, keyLabel, dx, dy),
        )
    }

    fun readRowCount(db: SQLiteDatabase): Int =
        db.rawQuery("SELECT COUNT(*) FROM ${HeatmapLearningDatabase_v1.TABLE_ALIGNMENT_OFFSET}", null)
            .use { c -> if (c.moveToFirst()) c.getInt(0) else 0 }
}
