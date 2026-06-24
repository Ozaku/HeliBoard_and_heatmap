// SPDX-License-Identifier: GPL-3.0-only

// ai-note: Block 3 step 16 — dev toggle; default OFF keeps legacy gesture path

package helium314.keyboard.heatmap.swipe

import android.content.Context
import helium314.keyboard.heatmap.learning.HeatmapCrossProcessPrefs_v2

object HeatmapLiteralSwipeSettings_v1 {

    const val PREF_USE_LITERAL_SWIPE_ENGINE = "heatmap_use_literal_swipe_engine"

    const val DEFAULT_USE_LITERAL_SWIPE_ENGINE = false

    @JvmStatic
    fun isLiteralSwipeEngineEnabled(context: Context): Boolean =
        HeatmapCrossProcessPrefs_v2.readPrefs(context).getBoolean(
            PREF_USE_LITERAL_SWIPE_ENGINE,
            DEFAULT_USE_LITERAL_SWIPE_ENGINE,
        )
}
