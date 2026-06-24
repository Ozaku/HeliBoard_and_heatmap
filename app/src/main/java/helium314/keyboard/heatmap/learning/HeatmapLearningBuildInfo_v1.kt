// SPDX-License-Identifier: GPL-3.0-only
// ai-note: heatmap learning release identity — bump BETA_VERSION per instrumentation sub-step
package helium314.keyboard.heatmap.learning

import helium314.keyboard.latin.BuildConfig

/**
 * Tracks the heatmap/smart-keyboard feature line separately from HeliBoard [BuildConfig.VERSION_NAME].
 */
object HeatmapLearningBuildInfo_v1 {
    /** User-facing beta label for heatmap learning milestones (e.g. 0.0.0.1). */
    const val BETA_VERSION: String = BuildConfig.HEATMAP_LEARNING_BETA

    /** Matches sub-step in ai_notes/26_BLOCK1_INSTRUMENTATION_SUBSTEPS_v1.txt */
    const val ROADMAP_STEP: Int = BuildConfig.HEATMAP_LEARNING_ROADMAP_STEP

    /** Single-line string for debug UI and export headers. */
    fun statusLine(): String = "Heatmap learning beta $BETA_VERSION (roadmap step $ROADMAP_STEP)"
}
