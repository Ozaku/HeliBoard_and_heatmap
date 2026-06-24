// SPDX-License-Identifier: GPL-3.0-only

// ai-note: Block 2 step 9 — persist coordinate map rows per locale + layout_hash



package helium314.keyboard.heatmap.learning



import android.content.ContentValues

import android.content.Context

import android.database.sqlite.SQLiteDatabase



object HeatmapKeyBoundsStore_v1 {

    private val lock = Any()



    fun ensurePersisted(context: Context, snapshot: HeatmapCoordinateMap_v1.Snapshot) {

        synchronized(lock) {

            if (hasBounds(context, snapshot.localeTag, snapshot.layoutHash)) return

            val db = writable(context)

            db.beginTransaction()

            try {

                for (key in snapshot.keys) {

                    val cv = ContentValues().apply {

                        put(HeatmapLearningDatabase_v1.COL_LOCALE, snapshot.localeTag)

                        put(HeatmapLearningDatabase_v1.COL_LAYOUT_HASH, snapshot.layoutHash)

                        put(HeatmapLearningDatabase_v1.COL_KEY_LABEL, key.storageLabel)

                        put(HeatmapLearningDatabase_v1.COL_KEY_CODE, key.keyCode)

                        put(HeatmapLearningDatabase_v1.COL_LEFT_PX, key.left)

                        put(HeatmapLearningDatabase_v1.COL_TOP_PX, key.top)

                        put(HeatmapLearningDatabase_v1.COL_RIGHT_PX, key.right)

                        put(HeatmapLearningDatabase_v1.COL_BOTTOM_PX, key.bottom)

                        put(HeatmapLearningDatabase_v1.COL_CENTER_X, key.centerX)

                        put(HeatmapLearningDatabase_v1.COL_CENTER_Y, key.centerY)

                    }

                    db.insert(HeatmapLearningDatabase_v1.TABLE_KEY_BOUNDS, null, cv)

                }

                db.setTransactionSuccessful()

            } finally {

                db.endTransaction()

            }

        }

    }



    fun readBoundsRowCount(context: Context, locale: String?, layoutHash: String?): Int {

        if (locale.isNullOrEmpty() || layoutHash.isNullOrEmpty()) return 0

        synchronized(lock) {

            return readable(context).rawQuery(

                "SELECT COUNT(*) FROM ${HeatmapLearningDatabase_v1.TABLE_KEY_BOUNDS} " +

                    "WHERE ${HeatmapLearningDatabase_v1.COL_LOCALE}=? AND ${HeatmapLearningDatabase_v1.COL_LAYOUT_HASH}=?",

                arrayOf(locale, layoutHash),

            ).use { c -> if (c.moveToFirst()) c.getInt(0) else 0 }

        }

    }



    fun readLatestLayoutHash(context: Context): String? = synchronized(lock) {

        readable(context).rawQuery(

            """

            SELECT ${HeatmapLearningDatabase_v1.COL_LAYOUT_HASH}

            FROM ${HeatmapLearningDatabase_v1.TABLE_KEY_BOUNDS}

            ORDER BY rowid DESC LIMIT 1

            """.trimIndent(),

            null,

        ).use { c -> if (c.moveToFirst()) c.getString(0) else null }

    }



    private fun hasBounds(context: Context, locale: String, layoutHash: String): Boolean =

        readBoundsRowCount(context, locale, layoutHash) > 0



    private fun writable(context: Context): SQLiteDatabase =

        HeatmapLearningDatabase_v1.getInstance(context).writableDatabase



    private fun readable(context: Context): SQLiteDatabase =

        HeatmapLearningDatabase_v1.getInstance(context).readableDatabase

}

