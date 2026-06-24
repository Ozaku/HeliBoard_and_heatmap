// SPDX-License-Identifier: GPL-3.0-only

// ai-note: Block 3 step 15 — pointer rules; bypass legacy fast-move gate when heatmap swipe ON

package helium314.keyboard.heatmap.swipe

import helium314.keyboard.keyboard.Key
import helium314.keyboard.keyboard.Keyboard
import helium314.keyboard.latin.settings.Settings
import helium314.keyboard.latin.utils.Log

object HeatmapSwipePointerPolicy_v1 {

    private const val TAG = "HeatmapSwipeStart"

    @JvmStatic
    fun isHeatmapSwipeActive(): Boolean = Settings.getValues().mHeatmapSwipeEnabled

    /** ai-note: heatmap uses HeatmapSwipeTapGate only; skip isStartOfAGesture distance/time gates. */
    @JvmStatic
    fun useRelaxedBatchStart(): Boolean = isHeatmapSwipeActive()

    @JvmStatic
    fun strokeQualifiesAsSwipe(
        startX: Int,
        startY: Int,
        currentX: Int,
        currentY: Int,
        startKey: Key?,
        keyboard: Keyboard?,
    ): Boolean {
        val keyWidth = keyboard?.mMostCommonKeyWidth?.coerceAtLeast(1) ?: 1
        val keyHeight = keyboard?.mMostCommonKeyHeight?.coerceAtLeast(1) ?: 1
        return HeatmapSwipeTapGate_v1.qualifiesAsSwipe(
            startX, startY, currentX, currentY, startKey, keyWidth, keyHeight,
        )
    }

    /**
     * While finger is down and span qualifies, do not emit per-key tap letters mid-stroke.
     * Fixes swipe paths that typed "hello" one letter at a time before batch armed.
     */
    @JvmStatic
    fun shouldSuppressMidStrokeTapKeys(
        startX: Int,
        startY: Int,
        currentX: Int,
        currentY: Int,
        isDetectingGesture: Boolean,
        inGesture: Boolean,
        startKey: Key?,
        keyboard: Keyboard?,
    ): Boolean {
        if (!isHeatmapSwipeActive() || !isDetectingGesture || inGesture) return false
        return strokeQualifiesAsSwipe(startX, startY, currentX, currentY, startKey, keyboard)
    }

    @JvmStatic
    fun logBatchArmed(source: String, startLabel: String?) {
        Log.i(TAG, "batch armed ($source) start=${startLabel ?: "?"} heatmap=${isHeatmapSwipeActive()}")
    }
}
