// SPDX-License-Identifier: GPL-3.0-only

// ai-note: Block 2 step 10 — curve/path signature bucket counts + first-seen polyline blob

package helium314.keyboard.heatmap.learning

import android.content.ContentValues
import android.database.sqlite.SQLiteDatabase

object HeatmapPathBucketStore_v1 {

    fun bumpBucket(
        db: SQLiteDatabase,
        locale: String,
        layoutHash: String,
        inputMode: String,
        signatureHash: String,
        polylineBlob: ByteArray,
    ) {
        val cv = ContentValues().apply {
            put(HeatmapLearningDatabase_v1.COL_LOCALE, locale)
            put(HeatmapLearningDatabase_v1.COL_LAYOUT_HASH, layoutHash)
            put(HeatmapLearningDatabase_v1.COL_INPUT_MODE, inputMode)
            put(HeatmapLearningDatabase_v1.COL_SIGNATURE_HASH, signatureHash)
            put(HeatmapLearningDatabase_v1.COL_SEEN_COUNT, 1)
            put(HeatmapLearningDatabase_v1.COL_POLYLINE_BLOB, polylineBlob)
        }
        val rowId = db.insertWithOnConflict(
            HeatmapLearningDatabase_v1.TABLE_PATH_BUCKET,
            null,
            cv,
            SQLiteDatabase.CONFLICT_IGNORE,
        )
        if (rowId >= 0) return
        db.execSQL(
            """
            UPDATE ${HeatmapLearningDatabase_v1.TABLE_PATH_BUCKET}
            SET ${HeatmapLearningDatabase_v1.COL_SEEN_COUNT} =
              ${HeatmapLearningDatabase_v1.COL_SEEN_COUNT} + 1
            WHERE ${HeatmapLearningDatabase_v1.COL_LOCALE} = ?
              AND ${HeatmapLearningDatabase_v1.COL_LAYOUT_HASH} = ?
              AND ${HeatmapLearningDatabase_v1.COL_INPUT_MODE} = ?
              AND ${HeatmapLearningDatabase_v1.COL_SIGNATURE_HASH} = ?
            """.trimIndent(),
            arrayOf<Any>(locale, layoutHash, inputMode, signatureHash),
        )
    }

    fun readRowCount(db: SQLiteDatabase): Int =
        db.rawQuery("SELECT COUNT(*) FROM ${HeatmapLearningDatabase_v1.TABLE_PATH_BUCKET}", null)
            .use { c -> if (c.moveToFirst()) c.getInt(0) else 0 }
}
