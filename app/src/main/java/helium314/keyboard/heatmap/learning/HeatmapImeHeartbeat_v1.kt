// SPDX-License-Identifier: GPL-3.0-only

// ai-note: proves Heatmap KB (beta) IME is active — written from LatinIME, read in settings

package helium314.keyboard.heatmap.learning



import android.content.Context



object HeatmapImeHeartbeat_v1 {

    private const val PREF_IME_PACKAGE = "heatmap_ime_last_package"

    private const val PREF_IME_BETA = "heatmap_ime_last_beta"

    private const val PREF_IME_TIME_MS = "heatmap_ime_last_time_ms"

    private const val PREF_TOTAL_COMMITS = "heatmap_total_commits_ever"



    @JvmStatic

    fun onImeStarted(context: Context) {

        HeatmapCrossProcessPrefs_v2.editCommit(context) {

            putString(PREF_IME_PACKAGE, context.applicationContext.packageName)

            putString(PREF_IME_BETA, HeatmapLearningBuildInfo_v1.BETA_VERSION)

            putLong(PREF_IME_TIME_MS, System.currentTimeMillis())

        }

    }



    fun totalCommits(context: Context): Int =

        HeatmapCrossProcessPrefs_v2.readPrefs(context).getInt(PREF_TOTAL_COMMITS, 0)



    fun formatStatusLine(context: Context): String {

        val prefs = HeatmapCrossProcessPrefs_v2.readPrefs(context)

        val pkg = prefs.getString(PREF_IME_PACKAGE, null)

        val beta = prefs.getString(PREF_IME_BETA, null)

        val time = prefs.getLong(PREF_IME_TIME_MS, 0L)

        val commits = prefs.getInt(PREF_TOTAL_COMMITS, 0)

        return when {

            pkg == null || time == 0L ->

                "IME not seen yet — enable Heatmap KB (beta) in system keyboard list, not store HeliBoard"

            else ->

                "IME active: $pkg beta $beta (last input ${formatAge(time)}), $commits commits in prefs"

        }

    }



    private fun formatAge(timeMs: Long): String {

        val sec = ((System.currentTimeMillis() - timeMs) / 1000).coerceAtLeast(0)

        return when {

            sec < 60 -> "${sec}s ago"

            sec < 3600 -> "${sec / 60}m ago"

            else -> "${sec / 3600}h ago"

        }

    }

}

