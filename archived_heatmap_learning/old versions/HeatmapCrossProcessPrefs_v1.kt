// SPDX-License-Identifier: GPL-3.0-only
// ai-note: IME and Settings run in different processes — avoid DeviceProtectedUtils static prefs cache when reading instrumentation
package helium314.keyboard.heatmap.learning

import android.content.Context
import android.content.SharedPreferences
import android.os.Build

object HeatmapCrossProcessPrefs_v1 {
    private fun prefsName(context: Context): String = context.packageName + "_preferences"

    /** Fresh SharedPreferences for reads from Settings after IME wrote commits. */
    fun readPrefs(context: Context): SharedPreferences {
        val ctx = deviceProtectedContext(context)
        return ctx.getSharedPreferences(prefsName(context), Context.MODE_PRIVATE)
    }

    fun editCommit(context: Context, block: SharedPreferences.Editor.() -> Unit) {
        readPrefs(context).edit().apply(block).commit()
    }

    private fun deviceProtectedContext(context: Context): Context {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) return context
        return if (context.isDeviceProtectedStorage) context
        else context.createDeviceProtectedStorageContext() ?: context
    }
}
