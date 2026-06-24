// SPDX-License-Identifier: GPL-3.0-only

// ai-note: Block 2 step 8 — SQLite schema for persistent heatmap learning (separate from heliboard.db)

package helium314.keyboard.heatmap.learning



import android.content.Context

import android.database.sqlite.SQLiteDatabase

import android.database.sqlite.SQLiteOpenHelper

import helium314.keyboard.latin.utils.Log

import java.io.File



class HeatmapLearningDatabase_v1 private constructor(

    context: Context,

    dbFile: File,

) : SQLiteOpenHelper(context.applicationContext, dbFile.absolutePath, null, SCHEMA_VERSION) {



    override fun onCreate(db: SQLiteDatabase) {

        db.execSQL(CREATE_META)

        db.execSQL(CREATE_KEY_STAT)

        db.execSQL(CREATE_WORD_PAIR)

        db.execSQL(CREATE_WORD_COMMIT)

        db.execSQL(CREATE_KEY_BOUNDS)

        db.execSQL(CREATE_PATH_BUCKET)

        db.execSQL(CREATE_ALIGNMENT_OFFSET)

        db.execSQL(CREATE_LETTER_CONFUSION)

        db.execSQL(CREATE_WORD_CORRECTION)

        db.execSQL("INSERT INTO $TABLE_META ($COL_META_ID, $COL_SCHEMA_VERSION, $COL_UPDATED_AT_MS) VALUES (1, $SCHEMA_VERSION, 0)")

    }



    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {

        if (oldVersion < 2) {

            Log.i(TAG, "heatmap DB upgrade $oldVersion -> $newVersion (add key_bounds)")

            db.execSQL(CREATE_KEY_BOUNDS)

        }

        if (oldVersion < 3) {

            Log.i(TAG, "heatmap DB upgrade $oldVersion -> $newVersion (add path_bucket + alignment_offset)")

            db.execSQL(CREATE_PATH_BUCKET)

            db.execSQL(CREATE_ALIGNMENT_OFFSET)

        }

        if (oldVersion < 4) {

            Log.i(TAG, "heatmap DB upgrade $oldVersion -> $newVersion (add correction weights)")

            db.execSQL(CREATE_LETTER_CONFUSION)

            db.execSQL(CREATE_WORD_CORRECTION)

        }

        db.execSQL(

            "UPDATE $TABLE_META SET ${COL_SCHEMA_VERSION}=$SCHEMA_VERSION, ${COL_UPDATED_AT_MS}=? WHERE $COL_META_ID=1",

            arrayOf(System.currentTimeMillis()),

        )

    }



    companion object {

        private const val TAG = "HeatmapInstr"

        const val SCHEMA_VERSION = 4

        const val DB_FILE_NAME = "heatmap_store.db"



        const val TABLE_META = "heatmap_meta"

        const val COL_META_ID = "id"

        const val COL_SCHEMA_VERSION = "schema_version"

        const val COL_UPDATED_AT_MS = "updated_at_ms"



        const val TABLE_KEY_STAT = "heatmap_key_stat"

        const val COL_LOCALE = "locale"

        const val COL_LAYOUT_HASH = "layout_hash"

        const val COL_INPUT_MODE = "input_mode"

        const val COL_KEY_LABEL = "key_label"

        const val COL_SEEN_COUNT = "seen_count"

        const val COL_CORRECTED_FROM = "corrected_from_count"



        const val TABLE_WORD_PAIR = "heatmap_word_pair"

        const val COL_PREV_WORD = "prev_word"

        const val COL_NEXT_WORD = "next_word"

        const val COL_PAIR_COUNT = "pair_count"



        const val TABLE_KEY_BOUNDS = "heatmap_key_bounds"

        const val COL_KEY_CODE = "key_code"

        const val COL_LEFT_PX = "left_px"

        const val COL_TOP_PX = "top_px"

        const val COL_RIGHT_PX = "right_px"

        const val COL_BOTTOM_PX = "bottom_px"

        const val COL_CENTER_X = "center_x"

        const val COL_CENTER_Y = "center_y"

        const val TABLE_PATH_BUCKET = "heatmap_path_bucket"

        const val COL_SIGNATURE_HASH = "signature_hash"

        const val COL_POLYLINE_BLOB = "polyline_blob"

        const val TABLE_ALIGNMENT_OFFSET = "heatmap_alignment_offset"

        const val COL_SAMPLE_COUNT = "sample_count"

        const val COL_SUM_DX = "sum_dx"

        const val COL_SUM_DY = "sum_dy"

        const val TABLE_LETTER_CONFUSION = "heatmap_letter_confusion"

        const val COL_FROM_LABEL = "from_label"

        const val COL_TO_LABEL = "to_label"

        const val COL_POSITION_BAND = "position_band"

        const val COL_EVENT_COUNT = "event_count"

        const val COL_WEIGHT_SUM = "weight_sum"

        const val COL_LAST_SEEN_MS = "last_seen_ms"

        const val COL_TYPED_TEXT = "typed_text"

        const val COL_COMMITTED_TEXT = "committed_text"

        const val TABLE_WORD_CORRECTION = "heatmap_word_correction"

        const val TABLE_WORD_COMMIT = "heatmap_word_commit"

        const val COL_COMMIT_AT = "committed_at_ms"

        const val COL_SLOT_ID = "slot_id"

        const val COL_HOST = "host_package"

        const val COL_COMMITTED = "committed_text"

        const val COL_TYPED = "typed_text"

        const val COL_SESSION_GEN = "session_generation"



        private const val CREATE_META = """

            CREATE TABLE $TABLE_META (

              $COL_META_ID INTEGER PRIMARY KEY CHECK ($COL_META_ID = 1),

              $COL_SCHEMA_VERSION INTEGER NOT NULL,

              $COL_UPDATED_AT_MS INTEGER NOT NULL

            )

        """



        private const val CREATE_KEY_STAT = """

            CREATE TABLE $TABLE_KEY_STAT (

              $COL_LOCALE TEXT NOT NULL,

              $COL_LAYOUT_HASH TEXT NOT NULL,

              $COL_INPUT_MODE TEXT NOT NULL,

              $COL_KEY_LABEL TEXT NOT NULL,

              $COL_SEEN_COUNT INTEGER NOT NULL DEFAULT 0,

              $COL_CORRECTED_FROM INTEGER NOT NULL DEFAULT 0,

              PRIMARY KEY ($COL_LOCALE, $COL_LAYOUT_HASH, $COL_INPUT_MODE, $COL_KEY_LABEL)

            )

        """



        private const val CREATE_WORD_PAIR = """

            CREATE TABLE $TABLE_WORD_PAIR (

              $COL_LOCALE TEXT NOT NULL,

              $COL_PREV_WORD TEXT NOT NULL,

              $COL_NEXT_WORD TEXT NOT NULL,

              $COL_PAIR_COUNT INTEGER NOT NULL DEFAULT 0,

              PRIMARY KEY ($COL_LOCALE, $COL_PREV_WORD, $COL_NEXT_WORD)

            )

        """



        private const val CREATE_WORD_COMMIT = """

            CREATE TABLE $TABLE_WORD_COMMIT (

              id INTEGER PRIMARY KEY AUTOINCREMENT,

              $COL_COMMIT_AT INTEGER NOT NULL,

              $COL_SLOT_ID INTEGER NOT NULL,

              $COL_SESSION_GEN INTEGER NOT NULL,

              $COL_HOST TEXT,

              $COL_INPUT_MODE TEXT NOT NULL,

              $COL_COMMITTED TEXT NOT NULL,

              $COL_TYPED TEXT NOT NULL

            )

        """



        private const val CREATE_PATH_BUCKET = """

            CREATE TABLE $TABLE_PATH_BUCKET (

              $COL_LOCALE TEXT NOT NULL,

              $COL_LAYOUT_HASH TEXT NOT NULL,

              $COL_INPUT_MODE TEXT NOT NULL,

              $COL_SIGNATURE_HASH TEXT NOT NULL,

              $COL_SEEN_COUNT INTEGER NOT NULL DEFAULT 0,

              $COL_POLYLINE_BLOB BLOB,

              PRIMARY KEY ($COL_LOCALE, $COL_LAYOUT_HASH, $COL_INPUT_MODE, $COL_SIGNATURE_HASH)

            )

        """



        private const val CREATE_ALIGNMENT_OFFSET = """

            CREATE TABLE $TABLE_ALIGNMENT_OFFSET (

              $COL_LOCALE TEXT NOT NULL,

              $COL_LAYOUT_HASH TEXT NOT NULL,

              $COL_INPUT_MODE TEXT NOT NULL,

              $COL_KEY_LABEL TEXT NOT NULL,

              $COL_SAMPLE_COUNT INTEGER NOT NULL DEFAULT 0,

              $COL_SUM_DX INTEGER NOT NULL DEFAULT 0,

              $COL_SUM_DY INTEGER NOT NULL DEFAULT 0,

              PRIMARY KEY ($COL_LOCALE, $COL_LAYOUT_HASH, $COL_INPUT_MODE, $COL_KEY_LABEL)

            )

        """



        private const val CREATE_LETTER_CONFUSION = """

            CREATE TABLE $TABLE_LETTER_CONFUSION (

              $COL_LOCALE TEXT NOT NULL,

              $COL_LAYOUT_HASH TEXT NOT NULL,

              $COL_INPUT_MODE TEXT NOT NULL,

              $COL_FROM_LABEL TEXT NOT NULL,

              $COL_TO_LABEL TEXT NOT NULL,

              $COL_POSITION_BAND TEXT NOT NULL,

              $COL_EVENT_COUNT INTEGER NOT NULL DEFAULT 0,

              $COL_WEIGHT_SUM INTEGER NOT NULL DEFAULT 0,

              $COL_LAST_SEEN_MS INTEGER NOT NULL DEFAULT 0,

              PRIMARY KEY ($COL_LOCALE, $COL_LAYOUT_HASH, $COL_INPUT_MODE,

                $COL_FROM_LABEL, $COL_TO_LABEL, $COL_POSITION_BAND)

            )

        """



        private const val CREATE_WORD_CORRECTION = """

            CREATE TABLE $TABLE_WORD_CORRECTION (

              $COL_LOCALE TEXT NOT NULL,

              $COL_INPUT_MODE TEXT NOT NULL,

              $COL_TYPED_TEXT TEXT NOT NULL,

              $COL_COMMITTED_TEXT TEXT NOT NULL,

              $COL_EVENT_COUNT INTEGER NOT NULL DEFAULT 0,

              $COL_WEIGHT_SUM INTEGER NOT NULL DEFAULT 0,

              $COL_LAST_SEEN_MS INTEGER NOT NULL DEFAULT 0,

              PRIMARY KEY ($COL_LOCALE, $COL_INPUT_MODE, $COL_TYPED_TEXT, $COL_COMMITTED_TEXT)

            )

        """



        private const val CREATE_KEY_BOUNDS = """

            CREATE TABLE $TABLE_KEY_BOUNDS (

              $COL_LOCALE TEXT NOT NULL,

              $COL_LAYOUT_HASH TEXT NOT NULL,

              $COL_KEY_LABEL TEXT NOT NULL,

              $COL_KEY_CODE INTEGER NOT NULL DEFAULT 0,

              $COL_LEFT_PX INTEGER NOT NULL,

              $COL_TOP_PX INTEGER NOT NULL,

              $COL_RIGHT_PX INTEGER NOT NULL,

              $COL_BOTTOM_PX INTEGER NOT NULL,

              $COL_CENTER_X INTEGER NOT NULL,

              $COL_CENTER_Y INTEGER NOT NULL,

              PRIMARY KEY ($COL_LOCALE, $COL_LAYOUT_HASH, $COL_KEY_LABEL)

            )

        """



        @Volatile

        private var instance: HeatmapLearningDatabase_v1? = null



        fun getInstance(context: Context): HeatmapLearningDatabase_v1 {

            return instance ?: synchronized(this) {

                instance ?: HeatmapLearningDatabase_v1(

                    context.applicationContext,

                    dbFile(context.applicationContext),

                ).also { instance = it }

            }

        }



        fun dbFile(context: Context): File =

            File(HeatmapLearningFiles_v2.exportDirectory(context), DB_FILE_NAME)



        /** Close singleton and delete DB; next [getInstance] recreates empty schema. */

        fun wipePersistentStore(context: Context) {

            synchronized(this) {

                instance?.close()

                instance = null

            }

            dbFile(context.applicationContext).delete()

        }



        /** Robolectric / unit tests only. */

        internal fun resetForTests(context: Context) = wipePersistentStore(context)

    }

}

