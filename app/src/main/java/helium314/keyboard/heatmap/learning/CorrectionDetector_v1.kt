// SPDX-License-Identifier: GPL-3.0-only

// ai-note: step 1.11 — flush hooks only; no field text diff yet (see 1.12)

package helium314.keyboard.heatmap.learning



import android.content.Context

import android.content.SharedPreferences

import helium314.keyboard.latin.utils.Log



object CorrectionDetector_v1 {

    private const val TAG = "HeatmapInstr"

    private const val PREF_LAST_FLUSH_REASON = "heatmap_last_flush_reason"

    private const val PREF_LAST_FLUSH_TIME_MS = "heatmap_last_flush_time_ms"

    private const val PREF_LAST_FLUSH_HOST = "heatmap_last_flush_host"

    private const val PREF_TOTAL_FLUSHES = "heatmap_total_flushes"



    @JvmStatic

    fun onFlush(

        context: Context,

        reason: CorrectionFlushReason_v1,

        hostPackage: String?,

        flushContext: CorrectionFlushContext_v1,

    ) {

        if (!HeatmapLearningSettings_v2.isLearningEnabled(context)) return

        val host = hostPackage ?: HeatmapWordSlotSession_v7.HOST_UNKNOWN_SENTINEL

        val total = HeatmapCrossProcessPrefs_v2.readPrefs(context).getInt(PREF_TOTAL_FLUSHES, 0) + 1

        HeatmapCrossProcessPrefs_v2.editCommit(context) {

            putString(PREF_LAST_FLUSH_REASON, reason.name)

            putLong(PREF_LAST_FLUSH_TIME_MS, System.currentTimeMillis())

            putString(PREF_LAST_FLUSH_HOST, host)

            putInt(PREF_TOTAL_FLUSHES, total)

        }

        Log.i(

            TAG,

            "flush ${reason.name} host=$host gen=${flushContext.sessionGeneration} " +

                "journalWords=${flushContext.journalWordCount} sessions=${flushContext.registrySessionCount}",

        )

    }



    fun formatStatusBlock(context: Context): String {

        val prefs = HeatmapCrossProcessPrefs_v2.readPrefs(context)

        val total = prefs.getInt(PREF_TOTAL_FLUSHES, 0)

        if (total <= 0) {

            return "\n\n— Last flush —\nNo flushes recorded yet (leave a field, switch apps, or press Send)."

        }

        val reason = prefs.getString(PREF_LAST_FLUSH_REASON, null) ?: "unknown"

        val host = prefs.getString(PREF_LAST_FLUSH_HOST, null) ?: "unknown"

        val time = prefs.getLong(PREF_LAST_FLUSH_TIME_MS, 0L)

        return buildString {

            append("\n\n— Last flush —")

            append("\nReason: ").append(reason)

            append("\nHost app: ").append(host)

            append("\nFlushes recorded: ").append(total)

            if (time > 0L) append("\nTime: ").append(formatAge(time))

        }

    }



    private fun formatAge(timeMs: Long): String {

        val sec = ((System.currentTimeMillis() - timeMs) / 1000).coerceAtLeast(0)

        return when {

            sec < 60 -> "${sec}s ago"

            sec < 3600 -> "${sec / 60}m ago"

            else -> "${sec / 3600}h ago"

        }

    }



    /** For unit tests. */

    internal fun readFlushTotal(prefs: SharedPreferences): Int =

        prefs.getInt(PREF_TOTAL_FLUSHES, 0)

}

