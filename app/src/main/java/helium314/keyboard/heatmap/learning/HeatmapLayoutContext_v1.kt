// SPDX-License-Identifier: GPL-3.0-only

// ai-note: Block 2 step 8 — locale tag now; layout hash placeholder until step 9

package helium314.keyboard.heatmap.learning



import android.content.Context

import helium314.keyboard.latin.settings.Settings



object HeatmapLayoutContext_v1 {

    const val LAYOUT_HASH_PLACEHOLDER: String = "layout_v0"



    @JvmStatic

    fun localeTag(context: Context): String = try {

        Settings.getValues().mLocale.toLanguageTag()

    } catch (_: Exception) {

        "und"

    }



    @JvmStatic

    fun layoutHash(context: Context): String = LAYOUT_HASH_PLACEHOLDER

}

