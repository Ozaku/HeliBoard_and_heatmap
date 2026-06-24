// SPDX-License-Identifier: GPL-3.0-only

// ai-note: v2 reads prefs fresh across IME vs Settings process (avoids stale learning toggle)

package helium314.keyboard.heatmap.learning



import android.content.Context



object HeatmapLearningSettings_v2 {

    fun isLearningEnabled(context: Context): Boolean =

        HeatmapLearningSettings_v1.isLearningEnabled(HeatmapCrossProcessPrefs_v2.readPrefs(context))



    fun getParagraphWindowChars(context: Context): Int =

        HeatmapLearningSettings_v1.getParagraphWindowChars(HeatmapCrossProcessPrefs_v2.readPrefs(context))

}

