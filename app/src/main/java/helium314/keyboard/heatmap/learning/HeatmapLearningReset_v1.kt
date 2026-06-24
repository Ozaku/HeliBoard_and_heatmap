// SPDX-License-Identifier: GPL-3.0-only

// ai-note: Block 2 step 11 — wipe SQLite, exports, instrumentation prefs; keep user toggles/slider



package helium314.keyboard.heatmap.learning



import android.content.Context

import helium314.keyboard.latin.utils.Log



object HeatmapLearningReset_v1 {

    private const val TAG = "HeatmapInstr"



    /** User-facing settings — not training data. */

    private val SETTINGS_KEYS_TO_KEEP = setOf(

        HeatmapLearningSettings_v1.PREF_LEARNING_ENABLED,

        HeatmapLearningSettings_v1.PREF_PARAGRAPH_WINDOW_CHARS,

        HeatmapAutocorrectSettings_v1.PREF_TAP_AUTOCORRECT_ENABLED,

        HeatmapAutocorrectSettings_v1.PREF_SWIPE_AUTOCORRECT_ENABLED,

        HeatmapExportSettings_v1.PREF_AUTO_FILE_DUMP,

    )



    /**

     * Deletes all heatmap training / instrumentation data on device.

     * Does not change learning master toggle, paragraph window, or autocorrect prefs.

     */

    @JvmStatic

    fun wipeAllTrainingData(context: Context): Boolean {

        val app = context.applicationContext

        return try {

            HeatmapLearningDatabase_v1.wipePersistentStore(app)

            deleteExportArtifacts(app)

            clearTrainingPrefs(app)

            HeatmapWordSlotSession_v7.clearInMemorySessionAfterTrainingWipe(app)

            Log.i(TAG, "wipeAllTrainingData ok")

            true

        } catch (e: Exception) {

            Log.e(TAG, "wipeAllTrainingData failed", e)

            false

        }

    }



    private fun deleteExportArtifacts(context: Context) {

        val dir = HeatmapLearningFiles_v2.exportDirectory(context)

        dir.listFiles()?.forEach { file ->

            if (!file.isFile) return@forEach

            val name = file.name

            if (name == HeatmapLearningDatabase_v1.DB_FILE_NAME) return@forEach

            if (name.startsWith("session_export") && name.endsWith(".json")) {

                file.delete()

            }

        }

    }



    private fun clearTrainingPrefs(context: Context) {

        val prefs = HeatmapCrossProcessPrefs_v2.readPrefs(context)

        val editor = prefs.edit()

        prefs.all.keys.forEach { key ->

            if (!key.startsWith("heatmap_")) return@forEach

            if (key in SETTINGS_KEYS_TO_KEEP) return@forEach

            editor.remove(key)

        }

        editor.commit()

    }

}

