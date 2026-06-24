// SPDX-License-Identifier: GPL-3.0-only

// ai-note: step 1.13 — atomic JSON writes; archived copies when auto file dump is on

package helium314.keyboard.heatmap.learning



import android.content.Context

import helium314.keyboard.latin.utils.Log

import java.io.File

import java.text.SimpleDateFormat

import java.util.Date

import java.util.Locale



object HeatmapSessionExportWriter_v1 {

    private const val TAG = "HeatmapInstr"

    private const val PREF_LAST_EXPORT_AT_MS = "heatmap_last_export_at_ms"

    private const val PREF_LAST_EXPORT_BYTES = "heatmap_last_export_bytes"

    private const val ARCHIVE_PREFIX = "session_export_"

    private const val MAX_ARCHIVES = 12



    fun writeLatest(context: Context, json: String): Boolean {

        HeatmapLearningFiles_v2.migrateFromDeviceProtectedIfNeeded(context)
        val file = HeatmapLearningFiles_v2.latestExportFile(context)

        val ok = writeAtomic(file, json)

        if (ok) {

            HeatmapCrossProcessPrefs_v2.editCommit(context) {

                putLong(PREF_LAST_EXPORT_AT_MS, System.currentTimeMillis())

                putInt(PREF_LAST_EXPORT_BYTES, json.length)

            }

            Log.i(TAG, "export latest ${file.absolutePath} bytes=${json.length}")

        }

        return ok

    }



    fun writeTimestampedArchive(context: Context, json: String) {

        if (!HeatmapExportSettings_v1.isAutoFileDumpEnabled(context)) return

        val dir = HeatmapLearningFiles_v2.exportDirectory(context)

        val stamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())

        val file = File(dir, "$ARCHIVE_PREFIX$stamp.json")

        if (writeAtomic(file, json)) {

            pruneArchives(dir)

            Log.i(TAG, "export archive ${file.name}")

        }

    }



    /** Ensures a shareable file exists; returns null if write failed. */

    fun ensureShareableFile(context: Context): File? {

        HeatmapLearningFiles_v2.migrateFromDeviceProtectedIfNeeded(context)
        val latest = HeatmapLearningFiles_v2.latestExportFile(context)

        if (latest.isFile && latest.length() > 0L) return latest

        val json = HeatmapInstrumentationJson_v1.buildPrefsSnapshotOnly(context)

        return if (writeLatest(context, json)) latest else null

    }



    fun formatLastExportLine(context: Context): String {

        val prefs = HeatmapCrossProcessPrefs_v2.readPrefs(context)

        val at = prefs.getLong(PREF_LAST_EXPORT_AT_MS, 0L)

        val bytes = prefs.getInt(PREF_LAST_EXPORT_BYTES, 0)

        val path = HeatmapLearningFiles_v2.latestExportFile(context).absolutePath

        return if (at <= 0L) {

            "No export file yet. Type in another app with learning on, then export."

        } else {

            "Last write: ${formatAge(at)} · $bytes bytes\n$path"

        }

    }



    private fun writeAtomic(file: File, json: String): Boolean {

        return try {

            file.parentFile?.mkdirs()

            val tmp = File(file.parentFile, "${file.name}.tmp")

            tmp.writeText(json, Charsets.UTF_8)

            if (file.exists()) file.delete()

            if (!tmp.renameTo(file)) {

                tmp.copyTo(file, overwrite = true)

                tmp.delete()

            }

            file.isFile

        } catch (e: Exception) {

            Log.w(TAG, "export write failed ${file.absolutePath}", e)

            false

        }

    }



    private fun pruneArchives(dir: File) {

        val archives = dir.listFiles { f ->

            f.isFile && f.name.startsWith(ARCHIVE_PREFIX) && f.name.endsWith(".json")

        }?.sortedByDescending { it.lastModified() } ?: return

        archives.drop(MAX_ARCHIVES).forEach { it.delete() }

    }



    private fun formatAge(timeMs: Long): String {

        val sec = ((System.currentTimeMillis() - timeMs) / 1000).coerceAtLeast(0)

        return when {

            sec < 60 -> "${sec}s ago"

            sec < 3600 -> "${sec / 60}m ago"

            else -> "${sec / 3600}h ago"

        }

    }

}

