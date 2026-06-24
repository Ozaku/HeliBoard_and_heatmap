// SPDX-License-Identifier: GPL-3.0-only

// ai-note: Block 3 step 14a — heatmap swipe vs legacy glide decode; mutual exclusion per 48_/49_

package helium314.keyboard.heatmap.swipe

import android.content.Context
import helium314.keyboard.latin.settings.SettingsValues
import helium314.keyboard.latin.utils.JniUtils

object HeatmapSwipeModePolicy_v1 {

    enum class Mode {
        /** Geometry + tap-dictionary decode (our engine). */
        HEATMAP,
        /** Legacy glide JNI decode (external / system gesture lib). */
        LEGACY_GLIDE,
        /** Swipe gestures disabled. */
        DISABLED,
    }

    /** ai-note: user-imported libjni_latinime.so blocks heatmap swipe (48_ verify #2). */
    @JvmStatic
    fun isBlockedByUserGestureLib(): Boolean = JniUtils.sUserImportedGestureLib

    @JvmStatic
    fun heatmapSwipePrefEnabled(context: Context): Boolean {
        if (isBlockedByUserGestureLib()) return false
        return HeatmapLiteralSwipeSettings_v2.isHeatmapSwipeEnabled(context)
    }

    @JvmStatic
    fun resolve(settings: SettingsValues): Mode {
        if (isBlockedByUserGestureLib()) {
            return if (settings.mLegacyGlideInputEnabled) Mode.LEGACY_GLIDE else Mode.DISABLED
        }
        if (settings.mHeatmapSwipeEnabled) return Mode.HEATMAP
        if (settings.mLegacyGlideInputEnabled) return Mode.LEGACY_GLIDE
        return Mode.DISABLED
    }

    @JvmStatic
    fun shouldHandleSwipeGestures(settings: SettingsValues): Boolean =
        resolve(settings) != Mode.DISABLED

    @JvmStatic
    fun useHeatmapDecode(settings: SettingsValues): Boolean =
        resolve(settings) == Mode.HEATMAP
}
