// SPDX-License-Identifier: GPL-3.0-only

// ai-note: v2 — always use applicationContext + device-protected prefs; log commit failures

package helium314.keyboard.heatmap.learning



import android.content.Context

import android.content.SharedPreferences

import android.os.Build

import helium314.keyboard.latin.utils.Log



object HeatmapCrossProcessPrefs_v2 {

    private const val TAG = "HeatmapInstr"



    private fun prefsName(app: Context): String = app.packageName + "_preferences"



    fun readPrefs(context: Context): SharedPreferences {

        val app = context.applicationContext

        return deviceProtectedContext(app).getSharedPreferences(prefsName(app), Context.MODE_PRIVATE)

    }



    /** @return false if disk commit failed — caller should log */

    fun editCommit(context: Context, block: SharedPreferences.Editor.() -> Unit): Boolean {

        val ok = readPrefs(context).edit().apply(block).commit()

        if (!ok) Log.w(TAG, "prefs commit failed pkg=${context.applicationContext.packageName}")

        return ok

    }



    private fun deviceProtectedContext(context: Context): Context {

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) return context

        return if (context.isDeviceProtectedStorage) context

        else context.createDeviceProtectedStorageContext() ?: context

    }

}

