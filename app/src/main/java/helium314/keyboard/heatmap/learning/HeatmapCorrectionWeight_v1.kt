// SPDX-License-Identifier: GPL-3.0-only

// ai-note: Block 2 step 12 — conservative weights from typed!=committed only; decode in Block 4

package helium314.keyboard.heatmap.learning

import android.database.sqlite.SQLiteDatabase
import helium314.keyboard.latin.utils.Log
object HeatmapCorrectionWeight_v1 {

    private const val TAG = "HeatmapInstr"

    const val LETTER_WEIGHT_CAP = 100

    const val WORD_WEIGHT_CAP = 50

    private const val DECAY_AFTER_MS = 30L * 24L * 60L * 60L * 1000L

    private const val DECAY_NUMERATOR = 9

    private const val DECAY_DENOMINATOR = 10

    data class LetterDiff(
        val fromLabel: String,
        val toLabel: String,
        val band: HeatmapLetterPositionBand_v1,
    )

    fun applyOnCommit(
        db: SQLiteDatabase,
        locale: String,
        layoutHash: String,
        inputMode: String,
        typed: String,
        committed: String,
        nowMs: Long,
    ) {
        if (typed == committed) return
        bumpWordCorrection(db, locale, inputMode, typed, committed, nowMs)
        for (diff in collectLetterDiffs(typed, committed)) {
            bumpLetterConfusion(db, locale, layoutHash, inputMode, diff, nowMs)
        }
        Log.i(TAG, "correction weight typed=${typed.take(24)} committed=${committed.take(24)} mode=$inputMode")
    }

    internal fun collectLetterDiffs(typed: String, committed: String): List<LetterDiff> {
        if (typed == committed) return emptyList()
        val span = maxOf(typed.length, committed.length).coerceAtLeast(1)
        val out = ArrayList<LetterDiff>()
        for (i in 0 until span) {
            val fromCh = typed.getOrNull(i)
            val toCh = committed.getOrNull(i)
            if (fromCh == null || toCh == null || fromCh == toCh) continue
            if (!fromCh.isLetter() || !toCh.isLetter()) continue
            val band = positionBand(i, span)
            out.add(
                LetterDiff(
                    fromLabel = fromCh.lowercaseChar().toString(),
                    toLabel = toCh.lowercaseChar().toString(),
                    band = band,
                ),
            )
        }
        return out
    }

    internal fun positionBand(index: Int, span: Int): HeatmapLetterPositionBand_v1 = when {
        index == 0 -> HeatmapLetterPositionBand_v1.FIRST
        index == span - 1 -> HeatmapLetterPositionBand_v1.LAST
        else -> HeatmapLetterPositionBand_v1.MIDDLE
    }

    internal fun bandIncrement(band: HeatmapLetterPositionBand_v1): Int = when (band) {
        HeatmapLetterPositionBand_v1.FIRST -> 1
        HeatmapLetterPositionBand_v1.LAST -> 2
        HeatmapLetterPositionBand_v1.MIDDLE -> 3
    }

    private fun bumpWordCorrection(
        db: SQLiteDatabase,
        locale: String,
        inputMode: String,
        typed: String,
        committed: String,
        nowMs: Long,
    ) {
        val existing = readWordRow(db, locale, inputMode, typed, committed)
        val decayed = applyDecay(existing?.weightSum ?: 0, existing?.lastSeenMs ?: 0L, nowMs)
        val nextWeight = (decayed + 1).coerceAtMost(WORD_WEIGHT_CAP)
        val nextCount = (existing?.eventCount ?: 0) + 1
        db.execSQL(
            """
            INSERT INTO ${HeatmapLearningDatabase_v1.TABLE_WORD_CORRECTION}
              (${HeatmapLearningDatabase_v1.COL_LOCALE}, ${HeatmapLearningDatabase_v1.COL_INPUT_MODE},
               ${HeatmapLearningDatabase_v1.COL_TYPED_TEXT}, ${HeatmapLearningDatabase_v1.COL_COMMITTED_TEXT},
               ${HeatmapLearningDatabase_v1.COL_EVENT_COUNT}, ${HeatmapLearningDatabase_v1.COL_WEIGHT_SUM},
               ${HeatmapLearningDatabase_v1.COL_LAST_SEEN_MS})
            VALUES (?, ?, ?, ?, ?, ?, ?)
            ON CONFLICT(${HeatmapLearningDatabase_v1.COL_LOCALE}, ${HeatmapLearningDatabase_v1.COL_INPUT_MODE},
              ${HeatmapLearningDatabase_v1.COL_TYPED_TEXT}, ${HeatmapLearningDatabase_v1.COL_COMMITTED_TEXT})
            DO UPDATE SET
              ${HeatmapLearningDatabase_v1.COL_EVENT_COUNT} = ?,
              ${HeatmapLearningDatabase_v1.COL_WEIGHT_SUM} = ?,
              ${HeatmapLearningDatabase_v1.COL_LAST_SEEN_MS} = ?
            """.trimIndent(),
            arrayOf<Any>(
                locale, inputMode, typed, committed, nextCount, nextWeight, nowMs,
                nextCount, nextWeight, nowMs,
            ),
        )
    }

    private fun bumpLetterConfusion(
        db: SQLiteDatabase,
        locale: String,
        layoutHash: String,
        inputMode: String,
        diff: LetterDiff,
        nowMs: Long,
    ) {
        val bandName = diff.band.name
        val existing = readLetterRow(db, locale, layoutHash, inputMode, diff.fromLabel, diff.toLabel, bandName)
        val decayed = applyDecay(existing?.weightSum ?: 0, existing?.lastSeenMs ?: 0L, nowMs)
        val increment = bandIncrement(diff.band)
        val nextWeight = (decayed + increment).coerceAtMost(LETTER_WEIGHT_CAP)
        val nextCount = (existing?.eventCount ?: 0) + 1
        db.execSQL(
            """
            INSERT INTO ${HeatmapLearningDatabase_v1.TABLE_LETTER_CONFUSION}
              (${HeatmapLearningDatabase_v1.COL_LOCALE}, ${HeatmapLearningDatabase_v1.COL_LAYOUT_HASH},
               ${HeatmapLearningDatabase_v1.COL_INPUT_MODE},
               ${HeatmapLearningDatabase_v1.COL_FROM_LABEL}, ${HeatmapLearningDatabase_v1.COL_TO_LABEL},
               ${HeatmapLearningDatabase_v1.COL_POSITION_BAND},
               ${HeatmapLearningDatabase_v1.COL_EVENT_COUNT}, ${HeatmapLearningDatabase_v1.COL_WEIGHT_SUM},
               ${HeatmapLearningDatabase_v1.COL_LAST_SEEN_MS})
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
            ON CONFLICT(${HeatmapLearningDatabase_v1.COL_LOCALE}, ${HeatmapLearningDatabase_v1.COL_LAYOUT_HASH},
              ${HeatmapLearningDatabase_v1.COL_INPUT_MODE}, ${HeatmapLearningDatabase_v1.COL_FROM_LABEL},
              ${HeatmapLearningDatabase_v1.COL_TO_LABEL}, ${HeatmapLearningDatabase_v1.COL_POSITION_BAND})
            DO UPDATE SET
              ${HeatmapLearningDatabase_v1.COL_EVENT_COUNT} = ?,
              ${HeatmapLearningDatabase_v1.COL_WEIGHT_SUM} = ?,
              ${HeatmapLearningDatabase_v1.COL_LAST_SEEN_MS} = ?
            """.trimIndent(),
            arrayOf<Any>(
                locale, layoutHash, inputMode, diff.fromLabel, diff.toLabel, bandName,
                nextCount, nextWeight, nowMs,
                nextCount, nextWeight, nowMs,
            ),
        )
    }

    internal fun applyDecay(weightSum: Int, lastSeenMs: Long, nowMs: Long): Int {
        if (weightSum <= 0 || lastSeenMs <= 0L) return weightSum
        if (nowMs - lastSeenMs < DECAY_AFTER_MS) return weightSum
        return (weightSum * DECAY_NUMERATOR) / DECAY_DENOMINATOR
    }

    fun readLetterConfusionRowCount(db: SQLiteDatabase): Int =
        countTable(db, HeatmapLearningDatabase_v1.TABLE_LETTER_CONFUSION)

    fun readWordCorrectionRowCount(db: SQLiteDatabase): Int =
        countTable(db, HeatmapLearningDatabase_v1.TABLE_WORD_CORRECTION)

    private data class WeightRow(val eventCount: Int, val weightSum: Int, val lastSeenMs: Long)

    private fun readWordRow(
        db: SQLiteDatabase,
        locale: String,
        inputMode: String,
        typed: String,
        committed: String,
    ): WeightRow? = db.rawQuery(
        """
        SELECT ${HeatmapLearningDatabase_v1.COL_EVENT_COUNT},
               ${HeatmapLearningDatabase_v1.COL_WEIGHT_SUM},
               ${HeatmapLearningDatabase_v1.COL_LAST_SEEN_MS}
        FROM ${HeatmapLearningDatabase_v1.TABLE_WORD_CORRECTION}
        WHERE ${HeatmapLearningDatabase_v1.COL_LOCALE}=?
          AND ${HeatmapLearningDatabase_v1.COL_INPUT_MODE}=?
          AND ${HeatmapLearningDatabase_v1.COL_TYPED_TEXT}=?
          AND ${HeatmapLearningDatabase_v1.COL_COMMITTED_TEXT}=?
        """.trimIndent(),
        arrayOf(locale, inputMode, typed, committed),
    ).use { c -> if (c.moveToFirst()) WeightRow(c.getInt(0), c.getInt(1), c.getLong(2)) else null }

    private fun readLetterRow(
        db: SQLiteDatabase,
        locale: String,
        layoutHash: String,
        inputMode: String,
        fromLabel: String,
        toLabel: String,
        band: String,
    ): WeightRow? = db.rawQuery(
        """
        SELECT ${HeatmapLearningDatabase_v1.COL_EVENT_COUNT},
               ${HeatmapLearningDatabase_v1.COL_WEIGHT_SUM},
               ${HeatmapLearningDatabase_v1.COL_LAST_SEEN_MS}
        FROM ${HeatmapLearningDatabase_v1.TABLE_LETTER_CONFUSION}
        WHERE ${HeatmapLearningDatabase_v1.COL_LOCALE}=?
          AND ${HeatmapLearningDatabase_v1.COL_LAYOUT_HASH}=?
          AND ${HeatmapLearningDatabase_v1.COL_INPUT_MODE}=?
          AND ${HeatmapLearningDatabase_v1.COL_FROM_LABEL}=?
          AND ${HeatmapLearningDatabase_v1.COL_TO_LABEL}=?
          AND ${HeatmapLearningDatabase_v1.COL_POSITION_BAND}=?
        """.trimIndent(),
        arrayOf(locale, layoutHash, inputMode, fromLabel, toLabel, band),
    ).use { c -> if (c.moveToFirst()) WeightRow(c.getInt(0), c.getInt(1), c.getLong(2)) else null }

    private fun countTable(db: SQLiteDatabase, table: String): Int =
        db.rawQuery("SELECT COUNT(*) FROM $table", null).use { c ->
            if (c.moveToFirst()) c.getInt(0) else 0
        }
}
