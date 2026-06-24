// SPDX-License-Identifier: GPL-3.0-only

// ai-note: v2 — credential filesDir so FileProvider can share (user_de broke Export JSON on Samsung)

package helium314.keyboard.heatmap.learning



import android.content.Context

import android.os.Build

import helium314.keyboard.latin.utils.Log

import java.io.File



object HeatmapLearningFiles_v2 {

    private const val TAG = "HeatmapInstr"

    const val DIR_NAME = "heatmap_learning"

    const val LATEST_EXPORT_NAME = "session_export_latest.json"



    fun exportDirectory(context: Context): File {

        val app = context.applicationContext

        return File(app.filesDir, DIR_NAME).apply { mkdirs() }

    }



    fun latestExportFile(context: Context): File =

        File(exportDirectory(context), LATEST_EXPORT_NAME)



    /** Copy exports written under device-protected storage (v1) after upgrade. */

    fun migrateFromDeviceProtectedIfNeeded(context: Context) {

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) return

        val latest = latestExportFile(context)

        if (latest.isFile && latest.length() > 0L) return

        val app = context.applicationContext

        val deContext = app.createDeviceProtectedStorageContext() ?: return

        val oldDir = File(deContext.filesDir, DIR_NAME)

        if (!oldDir.isDirectory) return

        val newDir = exportDirectory(context)

        var copied = 0

        oldDir.listFiles()?.forEach { oldFile ->

            if (!oldFile.isFile) return@forEach

            try {

                oldFile.copyTo(File(newDir, oldFile.name), overwrite = true)

                copied++

            } catch (e: Exception) {

                Log.w(TAG, "migrate export ${oldFile.name} failed", e)

            }

        }

        if (copied > 0) Log.i(TAG, "migrated $copied export file(s) from device-protected storage")

    }

}

