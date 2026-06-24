// SPDX-License-Identifier: GPL-3.0-only

// ai-note: per-mode Heatmap autocorrect toggles — default OFF = HeliBoard path unchanged (Block 5 wires behavior)

package helium314.keyboard.heatmap.learning



import android.content.Context



object HeatmapAutocorrectSettings_v1 {

    const val PREF_TAP_AUTOCORRECT_ENABLED = "heatmap_tap_autocorrect_enabled"

    const val PREF_SWIPE_AUTOCORRECT_ENABLED = "heatmap_swipe_autocorrect_enabled"



    /** Default OFF until Heatmap System-2 autocorrect is implemented (Block 5). */

    const val DEFAULT_TAP_AUTOCORRECT_ENABLED = false

    const val DEFAULT_SWIPE_AUTOCORRECT_ENABLED = false



    fun isTapHeatmapAutocorrectEnabled(context: Context): Boolean =

        HeatmapCrossProcessPrefs_v2.readPrefs(context).getBoolean(

            PREF_TAP_AUTOCORRECT_ENABLED,

            DEFAULT_TAP_AUTOCORRECT_ENABLED,

        )



    fun isSwipeHeatmapAutocorrectEnabled(context: Context): Boolean =

        HeatmapCrossProcessPrefs_v2.readPrefs(context).getBoolean(

            PREF_SWIPE_AUTOCORRECT_ENABLED,

            DEFAULT_SWIPE_AUTOCORRECT_ENABLED,

        )

}

