// SPDX-License-Identifier: GPL-3.0-only

// ai-note: routes tap vs swipe to HeliBoard AC vs Heatmap System-2 AC (latter active in Block 5+)

package helium314.keyboard.heatmap.learning



import android.content.Context



object HeatmapAutocorrectPolicy_v1 {

    /**

     * When false, committed text for this input mode uses HeliBoard's existing autocorrect

     * and Text correction settings only — Heatmap does not replace words.

     */

    @JvmStatic

    fun useHeliBoardAutocorrectForMode(context: Context, mode: WordSessionInputMode_v1): Boolean =

        !shouldApplyHeatmapAutocorrect(context, mode)



    /**

     * True only when user enabled Heatmap AC for this mode AND implementation is active.

     * Until Block 5, always false even if toggles are ON (HeliBoard path preserved).

     */

    @JvmStatic

    fun shouldApplyHeatmapAutocorrect(context: Context, mode: WordSessionInputMode_v1): Boolean {

        if (!HeatmapLearningSettings_v2.isLearningEnabled(context)) return false

        val userWantsHeatmap = when (mode) {

            WordSessionInputMode_v1.TAP -> HeatmapAutocorrectSettings_v1.isTapHeatmapAutocorrectEnabled(context)

            WordSessionInputMode_v1.SWIPE -> HeatmapAutocorrectSettings_v1.isSwipeHeatmapAutocorrectEnabled(context)

        }

        return userWantsHeatmap && isHeatmapAutocorrectImplemented()

    }



    /** User toggle state (for status UI) — independent of Block 5 implementation gate. */

    @JvmStatic

    fun isHeatmapAutocorrectArmed(context: Context, mode: WordSessionInputMode_v1): Boolean {

        if (!HeatmapLearningSettings_v2.isLearningEnabled(context)) return false

        return when (mode) {

            WordSessionInputMode_v1.TAP -> HeatmapAutocorrectSettings_v1.isTapHeatmapAutocorrectEnabled(context)

            WordSessionInputMode_v1.SWIPE -> HeatmapAutocorrectSettings_v1.isSwipeHeatmapAutocorrectEnabled(context)

        }

    }



    /** Flip to true when Block 5 step 22–25 lands. */

    private fun isHeatmapAutocorrectImplemented(): Boolean = false



    fun formatStatusLines(context: Context): String = buildString {

        append("\n\n— Autocorrect routing (beta) —")

        append("\nTap: ").append(routeLabel(context, WordSessionInputMode_v1.TAP))

        append("\nSwipe: ").append(routeLabel(context, WordSessionInputMode_v1.SWIPE))

        append("\nLearning still records either way when enabled above.")

    }



    private fun routeLabel(context: Context, mode: WordSessionInputMode_v1): String = when {

        !HeatmapLearningSettings_v2.isLearningEnabled(context) -> "learning off"

        !isHeatmapAutocorrectArmed(context, mode) -> "HeliBoard (toggle off)"

        shouldApplyHeatmapAutocorrect(context, mode) -> "Heatmap System-2"

        else -> "HeliBoard (Heatmap AC not active yet)"

    }

}

