// SPDX-License-Identifier: GPL-3.0-only

// ai-note: v9 — debug summary adds Block 2 step 9 layout map status



package helium314.keyboard.heatmap.learning



import android.content.Context



object HeatmapInstrumentationSnapshot_v9 {

    fun save(context: Context, summary: HeatmapInstrumentationSnapshot_v6.LiveSummary, lastSession: WordSession_v5?): Boolean =

        HeatmapInstrumentationSnapshot_v8.save(context, summary, lastSession)



    fun formatForDebugSummary(context: Context, liveInThisProcess: HeatmapInstrumentationSnapshot_v6.LiveSummary): String =

        HeatmapInstrumentationSnapshot_v8.formatForDebugSummary(context, liveInThisProcess)

}

