// SPDX-License-Identifier: GPL-3.0-only



// ai-note: Block 3 step 16 — heatmap swipe typing toggle; default ON; no gesture-lib requirement



package helium314.keyboard.heatmap.swipe



import android.content.Context

import helium314.keyboard.heatmap.learning.HeatmapCrossProcessPrefs_v2



object HeatmapLiteralSwipeSettings_v2 {



    const val PREF_USE_LITERAL_SWIPE_ENGINE = "heatmap_use_literal_swipe_engine"



    /** ai-note: beta 0.0.0.24 — ON by default so swipe works without external gesture lib. */

    const val DEFAULT_HEATMAP_SWIPE_ENABLED = true



    @JvmStatic

    fun isHeatmapSwipeEnabled(context: Context): Boolean =

        HeatmapCrossProcessPrefs_v2.readPrefs(context).getBoolean(

            PREF_USE_LITERAL_SWIPE_ENGINE,

            DEFAULT_HEATMAP_SWIPE_ENABLED,

        )

}


