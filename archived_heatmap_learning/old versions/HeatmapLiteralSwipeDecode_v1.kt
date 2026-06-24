// SPDX-License-Identifier: GPL-3.0-only

// ai-note: Block 3 steps 13–15 orchestrator — literal path re-ranks legacy gesture suggestions

package helium314.keyboard.heatmap.swipe

import helium314.keyboard.keyboard.Keyboard
import helium314.keyboard.latin.settings.Settings
import helium314.keyboard.latin.DictionaryFacilitator
import helium314.keyboard.latin.SuggestedWords.SuggestedWordInfo
import helium314.keyboard.latin.common.InputPointers
import helium314.keyboard.latin.utils.Log

object HeatmapLiteralSwipeDecode_v1 {

    private const val TAG = "HeatmapLiteralSwipe"

    data class DecodeResult(
        val ranked: List<SuggestedWordInfo>,
        val infer: HeatmapSwipeSegmentInfer_v1.Result?,
    )

    @JvmStatic
    fun rerankGestureSuggestions(
        keyboard: Keyboard,
        pointers: InputPointers,
        facilitator: DictionaryFacilitator,
        legacySuggestions: List<SuggestedWordInfo>,
    ): DecodeResult {
        if (!Settings.getValues().mUseLiteralSwipeEngine) {
            return DecodeResult(ranked = legacySuggestions, infer = null)
        }
        val infer = HeatmapSwipeSegmentInfer_v1.infer(keyboard, pointers)
        if (infer == null || infer.pathLetters.isEmpty()) {
            Log.i(TAG, "literal decode skip — no layout inference")
            return DecodeResult(ranked = legacySuggestions, infer = null)
        }
        Log.i(
            TAG,
            "beats=${infer.beatCount} path=${infer.pathLetters.joinToString("")} " +
                "start=${infer.startKeyLabel} end=${infer.endKeyLabel}",
        )
        val ranked = HeatmapDictionaryPrefix_v1.filterAndRank(
            facilitator = facilitator,
            suggestions = legacySuggestions,
            infer = infer,
        )
        val out = if (ranked.isEmpty()) legacySuggestions else ranked
        return DecodeResult(ranked = out, infer = infer)
    }
}
