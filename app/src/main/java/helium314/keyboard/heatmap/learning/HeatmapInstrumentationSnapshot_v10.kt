// SPDX-License-Identifier: GPL-3.0-only

// ai-note: v10 — debug summary includes Block 2 step 10 path bucket + alignment counts

package helium314.keyboard.heatmap.learning

import android.content.Context

object HeatmapInstrumentationSnapshot_v10 {

    fun save(context: Context, summary: HeatmapInstrumentationSnapshot_v6.LiveSummary, lastSession: WordSession_v5?): Boolean =

        HeatmapInstrumentationSnapshot_v9.save(context, summary, lastSession)

    fun formatForDebugSummary(context: Context, liveInThisProcess: HeatmapInstrumentationSnapshot_v6.LiveSummary): String =

        HeatmapInstrumentationSnapshot_v9.formatForDebugSummary(context, liveInThisProcess)

}
