// SPDX-License-Identifier: GPL-3.0-only

// ai-note: Block 2 step 11 — granular wipes; ALL delegates to HeatmapLearningReset_v1

package helium314.keyboard.heatmap.learning

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import helium314.keyboard.latin.utils.Log

object HeatmapLearningReset_v2 {

    private const val TAG = "HeatmapInstr"

    private val lock = Any()

    enum class Layer {
        SWIPE_PATH,
        GEOMETRY,
        TYPO_WEIGHTS,
        ALL,
    }

    @JvmStatic
    fun wipe(context: Context, layer: Layer): Boolean = when (layer) {
        Layer.ALL -> HeatmapLearningReset_v1.wipeAllTrainingData(context)
        Layer.SWIPE_PATH -> wipeSwipePathBiases(context)
        Layer.GEOMETRY -> wipeGeometryData(context)
        Layer.TYPO_WEIGHTS -> wipeTypoWeightsStub(context)
    }

    /** Path signature buckets + swipe-only alignment offsets. */
    @JvmStatic
    fun wipeSwipePathBiases(context: Context): Boolean = synchronized(lock) {
        runCatching {
            val db = writable(context)
            db.beginTransaction()
            try {
                db.delete(HeatmapLearningDatabase_v1.TABLE_PATH_BUCKET, null, null)
                db.delete(
                    HeatmapLearningDatabase_v1.TABLE_ALIGNMENT_OFFSET,
                    "${HeatmapLearningDatabase_v1.COL_INPUT_MODE}=?",
                    arrayOf(WordSessionInputMode_v1.SWIPE.name),
                )
                touchMeta(db)
                db.setTransactionSuccessful()
            } finally {
                db.endTransaction()
            }
            Log.i(TAG, "wipeSwipePathBiases ok")
            true
        }.getOrElse { e ->
            Log.e(TAG, "wipeSwipePathBiases failed", e)
            false
        }
    }

    /** Key bounds + all alignment offsets (tap and swipe finger-vs-center). */
    @JvmStatic
    fun wipeGeometryData(context: Context): Boolean = synchronized(lock) {
        runCatching {
            val db = writable(context)
            db.beginTransaction()
            try {
                db.delete(HeatmapLearningDatabase_v1.TABLE_KEY_BOUNDS, null, null)
                db.delete(HeatmapLearningDatabase_v1.TABLE_ALIGNMENT_OFFSET, null, null)
                touchMeta(db)
                db.setTransactionSuccessful()
            } finally {
                db.endTransaction()
            }
            Log.i(TAG, "wipeGeometryData ok")
            true
        }.getOrElse { e ->
            Log.e(TAG, "wipeGeometryData failed", e)
            false
        }
    }

    /** Correction weights (step 12); Block 6 naughty-list tables added later. */
    @JvmStatic
    fun wipeTypoWeightsStub(context: Context): Boolean = synchronized(lock) {
        runCatching {
            val db = writable(context)
            db.beginTransaction()
            try {
                db.delete(HeatmapLearningDatabase_v1.TABLE_LETTER_CONFUSION, null, null)
                db.delete(HeatmapLearningDatabase_v1.TABLE_WORD_CORRECTION, null, null)
                HeatmapUserProfile_v1.clear(context)
                touchMeta(db)
                db.setTransactionSuccessful()
            } finally {
                db.endTransaction()
            }
            Log.i(TAG, "wipeTypoWeights ok (letter_confusion + word_correction + personal_ledger)")
            true
        }.getOrElse { e ->
            Log.e(TAG, "wipeTypoWeights failed", e)
            false
        }
    }

    private fun writable(context: Context): SQLiteDatabase =
        HeatmapLearningDatabase_v1.getInstance(context.applicationContext).writableDatabase

    private fun touchMeta(db: SQLiteDatabase) {
        db.execSQL(
            "UPDATE ${HeatmapLearningDatabase_v1.TABLE_META} SET " +
                "${HeatmapLearningDatabase_v1.COL_UPDATED_AT_MS}=?, " +
                "${HeatmapLearningDatabase_v1.COL_SCHEMA_VERSION}=? " +
                "WHERE ${HeatmapLearningDatabase_v1.COL_META_ID}=1",
            arrayOf<Any>(System.currentTimeMillis(), HeatmapLearningDatabase_v1.SCHEMA_VERSION),
        )
    }
}
