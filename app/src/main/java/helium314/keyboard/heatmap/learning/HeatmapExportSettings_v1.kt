// SPDX-License-Identifier: GPL-3.0-only

// ai-note: step 1.13 — auto timestamped JSON archives on flush (latest.json always updated on commit)

package helium314.keyboard.heatmap.learning



import android.content.Context



object HeatmapExportSettings_v1 {

    const val PREF_AUTO_FILE_DUMP = "heatmap_auto_file_dump"

    const val DEFAULT_AUTO_FILE_DUMP = false



    fun isAutoFileDumpEnabled(context: Context): Boolean =

        HeatmapCrossProcessPrefs_v2.readPrefs(context).getBoolean(

            PREF_AUTO_FILE_DUMP,

            DEFAULT_AUTO_FILE_DUMP,

        )

}

