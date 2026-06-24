// SPDX-License-Identifier: GPL-3.0-only

// ai-note: step 1.13 — ACTION_CREATE_DOCUMENT (Files/Downloads) not ACTION_SEND (Gmail/Discord)

package helium314.keyboard.heatmap.learning



import android.content.Context

import android.content.Intent

import android.net.Uri

import helium314.keyboard.latin.utils.Log

import java.text.SimpleDateFormat

import java.util.Date

import java.util.Locale



object HeatmapExportSave_v1 {

    private const val TAG = "HeatmapInstr"



    fun buildSaveIntent(context: Context): Intent {

        val stamp = SimpleDateFormat("yyyy-MM-dd_HHmm", Locale.US).format(Date())

        return Intent(Intent.ACTION_CREATE_DOCUMENT)

            .addCategory(Intent.CATEGORY_OPENABLE)

            .setType("application/json")

            .putExtra(Intent.EXTRA_TITLE, "heatmap_session_export_$stamp.json")

    }



    /** @return false if source missing or write failed */

    fun writeExportToUri(context: Context, destUri: Uri): Boolean {

        val source = HeatmapSessionExportWriter_v1.ensureShareableFile(context) ?: return false

        return try {

            val json = source.readText(Charsets.UTF_8)

            val out = context.contentResolver.openOutputStream(destUri) ?: return false

            out.use { stream ->

                stream.write(json.toByteArray(Charsets.UTF_8))

            }

            true

        } catch (e: Exception) {

            Log.e(TAG, "save export to $destUri failed", e)

            false

        }

    }

}

