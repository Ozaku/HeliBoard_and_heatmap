// SPDX-License-Identifier: GPL-3.0-only

// ai-note: v8 — debug summary adds Block 2 SQLite learning store counts



package helium314.keyboard.heatmap.learning



import android.content.Context



object HeatmapInstrumentationSnapshot_v8 {

    fun save(context: Context, summary: HeatmapInstrumentationSnapshot_v6.LiveSummary, lastSession: WordSession_v5?): Boolean =

        HeatmapInstrumentationSnapshot_v7.save(context, summary, lastSession)



    fun formatForDebugSummary(context: Context, liveInThisProcess: HeatmapInstrumentationSnapshot_v6.LiveSummary): String =

        HeatmapInstrumentationSnapshot_v7.formatForDebugSummary(context, liveInThisProcess) +

            HeatmapLearningStore_v1.formatStatusBlock(context)

}

