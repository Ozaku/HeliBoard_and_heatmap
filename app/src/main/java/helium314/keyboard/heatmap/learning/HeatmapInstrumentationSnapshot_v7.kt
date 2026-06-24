// SPDX-License-Identifier: GPL-3.0-only

// ai-note: v7 — commit snapshot (v6) + correction flush status block

package helium314.keyboard.heatmap.learning



import android.content.Context



object HeatmapInstrumentationSnapshot_v7 {

    fun save(context: Context, summary: HeatmapInstrumentationSnapshot_v6.LiveSummary, lastSession: WordSession_v5?): Boolean =

        HeatmapInstrumentationSnapshot_v6.save(context, summary, lastSession)



    fun formatForDebugSummary(context: Context, liveInThisProcess: HeatmapInstrumentationSnapshot_v6.LiveSummary): String =

        HeatmapInstrumentationSnapshot_v6.formatForDebugSummary(context, liveInThisProcess) +

            CorrectionDetector_v1.formatStatusBlock(context) +
            InWindowEditDetector_v1.formatStatusBlock(context) +
            HeatmapAutocorrectPolicy_v1.formatStatusLines(context) +
            HeatmapLearningGate_v1.formatStatusBlock(context) +
            HeatmapMetricsRecorder_v1.formatStatusBlock(context)

}

