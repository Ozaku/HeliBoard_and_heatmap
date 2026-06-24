// SPDX-License-Identifier: GPL-3.0-only

// ai-note: Block 2 step 8/10/12 — commits, path buckets, correction weights on typed!=committed

package helium314.keyboard.heatmap.learning



import android.content.ContentValues

import android.content.Context

import android.database.sqlite.SQLiteDatabase

import helium314.keyboard.latin.utils.Log



object HeatmapLearningStore_v1 {

    private const val TAG = "HeatmapInstr"

    private val lock = Any()



    @JvmStatic

    /** Caller must already pass [HeatmapLearningGate_v1] (IME commit path). */
    fun recordWordCommit(context: Context, session: WordSession_v5, previousWord: String?) {

        val layoutSnapshot = HeatmapLayoutContext_v2.captureForCommit(context)

        val locale = layoutSnapshot?.localeTag ?: HeatmapLayoutContext_v2.localeTag(context)

        val layoutHash = layoutSnapshot?.layoutHash ?: HeatmapLayoutContext_v2.layoutHash(context)

        layoutSnapshot?.let { HeatmapKeyBoundsStore_v1.ensurePersisted(context, it) }

        val mode = session.inputMode.name

        synchronized(lock) {

            val db = writable(context)

            db.beginTransaction()

            try {

                insertCommitRow(db, session, locale, layoutHash, mode)

                bumpKeyStat(db, locale, layoutHash, mode, session.committedText, session.typedText, layoutSnapshot)

                if (session.typedText != session.committedText) {

                    HeatmapCorrectionWeight_v1.applyOnCommit(

                        db = db,

                        locale = locale,

                        layoutHash = layoutHash,

                        inputMode = mode,

                        typed = session.typedText,

                        committed = session.committedText,

                        nowMs = session.committedAtMs,

                    )

                }

                if (!previousWord.isNullOrEmpty() && session.committedText.isNotEmpty()) {

                    bumpWordPair(db, locale, previousWord, session.committedText)

                }

                val pathStash = HeatmapPathCapture_v3.consume()

                if (pathStash != null && layoutSnapshot != null && session.inputMode == WordSessionInputMode_v1.SWIPE) {

                    HeatmapPathRecord_v2.recordSwipeCommit(

                        db = db,

                        locale = locale,

                        layoutHash = layoutHash,

                        inputMode = mode,

                        stash = pathStash,

                        layout = layoutSnapshot,

                    )

                }

                touchMeta(db)

                db.setTransactionSuccessful()

            } finally {

                db.endTransaction()

            }

        }

        Log.i(TAG, "store commit slot=${session.slotId.value} mode=$mode locale=$locale")

    }



    @JvmStatic

    fun readPersistedCommitCount(context: Context): Int = synchronized(lock) {

        readable(context).rawQuery(

            "SELECT COUNT(*) FROM ${HeatmapLearningDatabase_v1.TABLE_WORD_COMMIT}",

            null,

        ).use { c -> if (c.moveToFirst()) c.getInt(0) else 0 }

    }



    fun formatStatusBlock(context: Context): String = synchronized(lock) {

        val path = HeatmapLearningDatabase_v1.dbFile(context).absolutePath

        val commits = readPersistedCommitCount(context)

        val keys = countTable(readable(context), HeatmapLearningDatabase_v1.TABLE_KEY_STAT)

        val pairs = countTable(readable(context), HeatmapLearningDatabase_v1.TABLE_WORD_PAIR)

        buildString {

            append("\n\n— Learning store (Block 2) —")

            append("\nDB: ").append(path)

            append("\nSchema: v").append(HeatmapLearningDatabase_v1.SCHEMA_VERSION)

            append("\nPersisted commits: ").append(commits)

            append("\nKey stat rows: ").append(keys)

            append("\nWord pair rows: ").append(pairs)

            append("\nPath bucket rows: ").append(countTable(readable(context), HeatmapLearningDatabase_v1.TABLE_PATH_BUCKET))

            append("\nAlignment offset rows: ").append(countTable(readable(context), HeatmapLearningDatabase_v1.TABLE_ALIGNMENT_OFFSET))

            append("\nLetter confusion rows: ").append(countTable(readable(context), HeatmapLearningDatabase_v1.TABLE_LETTER_CONFUSION))

            append("\nWord correction rows: ").append(countTable(readable(context), HeatmapLearningDatabase_v1.TABLE_WORD_CORRECTION))

            append(HeatmapLayoutContext_v2.formatStatusBlock(context))

            append("\n(Survives app restart — type, force-stop, reopen, refresh status)")

        }

    }



    data class StoreSnapshot(

        val commitCount: Int,

        val keyStatRows: Int,

        val wordPairRows: Int,

        val keyBoundsRows: Int,

        val pathBucketRows: Int,

        val alignmentOffsetRows: Int,

        val letterConfusionRows: Int,

        val wordCorrectionRows: Int,

        val layoutHash: String?,

        val dbPath: String,

    )



    fun readSnapshot(context: Context): StoreSnapshot = synchronized(lock) {

        val db = readable(context)

        val layoutHash = HeatmapLayoutContext_v2.layoutHash(context)

        StoreSnapshot(

            commitCount = readPersistedCommitCount(context),

            keyStatRows = countTable(db, HeatmapLearningDatabase_v1.TABLE_KEY_STAT),

            wordPairRows = countTable(db, HeatmapLearningDatabase_v1.TABLE_WORD_PAIR),

            keyBoundsRows = countTable(db, HeatmapLearningDatabase_v1.TABLE_KEY_BOUNDS),

            pathBucketRows = countTable(db, HeatmapLearningDatabase_v1.TABLE_PATH_BUCKET),

            alignmentOffsetRows = countTable(db, HeatmapLearningDatabase_v1.TABLE_ALIGNMENT_OFFSET),

            letterConfusionRows = countTable(db, HeatmapLearningDatabase_v1.TABLE_LETTER_CONFUSION),

            wordCorrectionRows = countTable(db, HeatmapLearningDatabase_v1.TABLE_WORD_CORRECTION),

            layoutHash = layoutHash,

            dbPath = HeatmapLearningDatabase_v1.dbFile(context).absolutePath,

        )

    }



    private fun writable(context: Context): SQLiteDatabase =

        HeatmapLearningDatabase_v1.getInstance(context).writableDatabase



    private fun readable(context: Context): SQLiteDatabase =

        HeatmapLearningDatabase_v1.getInstance(context).readableDatabase



    private fun insertCommitRow(

        db: SQLiteDatabase,

        session: WordSession_v5,

        locale: String,

        layoutHash: String,

        mode: String,

    ) {

        val cv = ContentValues().apply {

            put(HeatmapLearningDatabase_v1.COL_COMMIT_AT, session.committedAtMs)

            put(HeatmapLearningDatabase_v1.COL_SLOT_ID, session.slotId.value)

            put(HeatmapLearningDatabase_v1.COL_SESSION_GEN, session.sessionGeneration)

            put(HeatmapLearningDatabase_v1.COL_HOST, session.hostPackage)

            put(HeatmapLearningDatabase_v1.COL_INPUT_MODE, mode)

            put(HeatmapLearningDatabase_v1.COL_COMMITTED, session.committedText)

            put(HeatmapLearningDatabase_v1.COL_TYPED, session.typedText)

        }

        db.insert(HeatmapLearningDatabase_v1.TABLE_WORD_COMMIT, null, cv)

    }



    private fun bumpKeyStat(

        db: SQLiteDatabase,

        locale: String,

        layoutHash: String,

        mode: String,

        committed: String,

        typed: String,

        layoutSnapshot: HeatmapCoordinateMap_v1.Snapshot?,

    ) {

        val label = layoutSnapshot?.keyLabelForFirstLetter(committed) ?: keyLabelProxy(committed) ?: return

        val corrected = typed != committed

        db.execSQL(

            """

            INSERT INTO ${HeatmapLearningDatabase_v1.TABLE_KEY_STAT}

              (${HeatmapLearningDatabase_v1.COL_LOCALE}, ${HeatmapLearningDatabase_v1.COL_LAYOUT_HASH},

               ${HeatmapLearningDatabase_v1.COL_INPUT_MODE}, ${HeatmapLearningDatabase_v1.COL_KEY_LABEL},

               ${HeatmapLearningDatabase_v1.COL_SEEN_COUNT}, ${HeatmapLearningDatabase_v1.COL_CORRECTED_FROM})

            VALUES (?, ?, ?, ?, 1, ?)

            ON CONFLICT(${HeatmapLearningDatabase_v1.COL_LOCALE}, ${HeatmapLearningDatabase_v1.COL_LAYOUT_HASH},

              ${HeatmapLearningDatabase_v1.COL_INPUT_MODE}, ${HeatmapLearningDatabase_v1.COL_KEY_LABEL})

            DO UPDATE SET

              ${HeatmapLearningDatabase_v1.COL_SEEN_COUNT} = ${HeatmapLearningDatabase_v1.COL_SEEN_COUNT} + 1,

              ${HeatmapLearningDatabase_v1.COL_CORRECTED_FROM} = ${HeatmapLearningDatabase_v1.COL_CORRECTED_FROM} + excluded.${HeatmapLearningDatabase_v1.COL_CORRECTED_FROM}

            """.trimIndent(),

            arrayOf<Any>(locale, layoutHash, mode, label, if (corrected) 1 else 0),

        )

    }



    private fun bumpWordPair(db: SQLiteDatabase, locale: String, prev: String, next: String) {

        db.execSQL(

            """

            INSERT INTO ${HeatmapLearningDatabase_v1.TABLE_WORD_PAIR}

              (${HeatmapLearningDatabase_v1.COL_LOCALE}, ${HeatmapLearningDatabase_v1.COL_PREV_WORD},

               ${HeatmapLearningDatabase_v1.COL_NEXT_WORD}, ${HeatmapLearningDatabase_v1.COL_PAIR_COUNT})

            VALUES (?, ?, ?, 1)

            ON CONFLICT(${HeatmapLearningDatabase_v1.COL_LOCALE}, ${HeatmapLearningDatabase_v1.COL_PREV_WORD},

              ${HeatmapLearningDatabase_v1.COL_NEXT_WORD})

            DO UPDATE SET ${HeatmapLearningDatabase_v1.COL_PAIR_COUNT} = ${HeatmapLearningDatabase_v1.COL_PAIR_COUNT} + 1

            """.trimIndent(),

            arrayOf<Any>(locale, prev, next),

        )

    }



    private fun touchMeta(db: SQLiteDatabase) {

        db.execSQL(

            "UPDATE ${HeatmapLearningDatabase_v1.TABLE_META} SET ${HeatmapLearningDatabase_v1.COL_UPDATED_AT_MS}=?, " +

                "${HeatmapLearningDatabase_v1.COL_SCHEMA_VERSION}=? WHERE ${HeatmapLearningDatabase_v1.COL_META_ID}=1",

            arrayOf<Any>(System.currentTimeMillis(), HeatmapLearningDatabase_v1.SCHEMA_VERSION),

        )

    }



    /** Until tap key indices land (step 9+), use first letter of committed word. */

    private fun keyLabelProxy(word: String): String? {

        val c = word.firstOrNull() ?: return null

        if (!c.isLetter()) return null

        return c.lowercaseChar().toString()

    }



    private fun countTable(db: SQLiteDatabase, table: String): Int =

        db.rawQuery("SELECT COUNT(*) FROM $table", null).use { c ->

            if (c.moveToFirst()) c.getInt(0) else 0

        }

}

