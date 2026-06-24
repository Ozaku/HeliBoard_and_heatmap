// SPDX-License-Identifier: GPL-3.0-only

// ai-note: deprecated — use HeatmapExportSave_v1 (CREATE_DOCUMENT); kept for reference only

package helium314.keyboard.heatmap.learning



import android.content.Context

import android.content.Intent

import androidx.core.content.FileProvider

import helium314.keyboard.latin.R
import helium314.keyboard.latin.utils.Log



object HeatmapExportShare_v1 {

    private const val TAG = "HeatmapInstr"

    /** @return false if no file or no handler */

    fun shareLatest(context: Context): Boolean {

        val file = HeatmapSessionExportWriter_v1.ensureShareableFile(context) ?: return false

        val authority = context.getString(R.string.gesture_data_provider_authority)

        return try {

            val uri = FileProvider.getUriForFile(context, authority, file)

            val intent = Intent(Intent.ACTION_SEND).apply {

                type = "application/json"

                putExtra(Intent.EXTRA_STREAM, uri)

                putExtra(Intent.EXTRA_SUBJECT, "heatmap_session_export.json")

                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)

            }

            val chooser = Intent.createChooser(intent, context.getString(R.string.heatmap_export_chooser_title))

            if (chooser.resolveActivity(context.packageManager) == null) return false

            context.startActivity(chooser)

            true

        } catch (e: IllegalArgumentException) {

            Log.e(TAG, "FileProvider cannot share ${file.absolutePath}", e)

            false

        }

    }

}

