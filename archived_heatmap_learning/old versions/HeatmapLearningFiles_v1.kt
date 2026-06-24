// SPDX-License-Identifier: GPL-3.0-only
// ai-note: device-protected files dir — same storage policy as HeatmapCrossProcessPrefs_v2
package helium314.keyboard.heatmap.learning

import android.content.Context
import android.os.Build
import java.io.File

object HeatmapLearningFiles_v1 {
    const val DIR_NAME = "heatmap_learning"
    const val LATEST_EXPORT_NAME = "session_export_latest.json"

    fun exportDirectory(context: Context): File {
        val app = deviceProtectedContext(context.applicationContext)
        return File(app.filesDir, DIR_NAME).apply { mkdirs() }
    }

    fun latestExportFile(context: Context): File =
        File(exportDirectory(context), LATEST_EXPORT_NAME)

    private fun deviceProtectedContext(context: Context): Context {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) return context
        return if (context.isDeviceProtectedStorage) context
        else context.createDeviceProtectedStorageContext() ?: context
    }
}
