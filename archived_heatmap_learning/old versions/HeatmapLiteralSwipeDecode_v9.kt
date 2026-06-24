// SPDX-License-Identifier: GPL-3.0-only

// ai-note: Block 3 step 15h — preview cache + lift projection + slot reject + light neighbor expand

package helium314.keyboard.heatmap.swipe

import helium314.keyboard.keyboard.Keyboard
import helium314.keyboard.latin.DictionaryFacilitator
import helium314.keyboard.latin.NgramContext
import helium314.keyboard.latin.SuggestedWords
import helium314.keyboard.latin.SuggestedWords.SuggestedWordInfo
import helium314.keyboard.latin.common.InputPointers
import helium314.keyboard.latin.settings.SettingsValuesForSuggestion
import helium314.keyboard.latin.utils.Log

object HeatmapLiteralSwipeDecode_v9 {

    private const val TAG = "HeatmapLiteralSwipe"

    data class DecodeResult(
        val ranked: List<SuggestedWordInfo>,
        val infer: HeatmapSwipeSegmentInfer_v6.Result?,
    )

    private var previewCacheKey: String? = null
    private var previewCache: DecodeResult? = null

    @JvmStatic
    fun decode(
        keyboard: Keyboard,
        pointers: InputPointers,
        facilitator: DictionaryFacilitator,
        ngramContext: NgramContext,
        settings: SettingsValuesForSuggestion,
        inputStyle: Int,
    ): DecodeResult {
        try {
            return decodeInternal(
                keyboard, pointers, facilitator, ngramContext, settings, inputStyle,
            )
        } catch (e: Exception) {
            Log.e(TAG, "decode crashed — returning empty", e)
            return DecodeResult(ranked = emptyList(), infer = null)
        }
    }

    private fun decodeInternal(
        keyboard: Keyboard,
        pointers: InputPointers,
        facilitator: DictionaryFacilitator,
        ngramContext: NgramContext,
        settings: SettingsValuesForSuggestion,
        inputStyle: Int,
    ): DecodeResult {
        val isPreview = inputStyle == SuggestedWords.INPUT_STYLE_UPDATE_BATCH
        val isTail = inputStyle == SuggestedWords.INPUT_STYLE_TAIL_BATCH
        val infer = HeatmapSwipeSegmentInfer_v6.infer(keyboard, pointers)
        if (infer == null || infer.pathLetters.isEmpty()) {
            Log.i(TAG, "decode skip — no geometry inference")
            return DecodeResult(ranked = emptyList(), infer = null)
        }
        val cacheKey = infer.pathLetters.joinToString("") + "|" + infer.endKeyLabel + "|" + infer.beatCount
        if (isPreview && previewCacheKey == cacheKey && previewCache != null) {
            return previewCache!!
        }
        if (isTail) {
            previewCacheKey = null
            previewCache = null
        }
        val pathEnd = infer.pathLetters.lastOrNull()
        val liftNote = if (pathEnd != infer.endKeyLabel) " liftEnd=${infer.endKeyLabel}" else ""
        val mode = if (isPreview) "preview" else "full"
        Log.i(
            TAG,
            "mode=$mode shape=${infer.straightLine.shape} maxLen=${infer.maxWordLength} " +
                "beatsRaw=${infer.beatCountRaw} beats=${infer.beatCount} " +
                "pathRaw=${infer.pathLettersRaw.joinToString("")} " +
                "path=${infer.pathLetters.joinToString("")} " +
                "start=${infer.startKeyLabel} end=${infer.endKeyLabel}$liftNote",
        )
        val prefixes = HeatmapSwipePrefixEngine_v7.buildPrefixVariants(infer)
        val scored = HeatmapSwipeCandidateSource_v9.collectDictionaryCandidates(
            prefixes = prefixes,
            infer = infer,
            facilitator = facilitator,
            ngramContext = ngramContext,
            keyboard = keyboard,
            settings = settings,
            inputStyle = inputStyle,
        )
        if (scored.isEmpty()) {
            Log.i(TAG, "decode — no dictionary matches for prefixes=$prefixes")
            HeatmapSwipeDecodeSnapshot_v1.stash(infer.pathLettersRaw, infer.pathLetters, emptyList())
            return DecodeResult(ranked = emptyList(), infer = infer)
        }
        val ranked = HeatmapSwipeSlotRejectMemory_v1.filterRanked(
            scored.map { (info, combined) -> bumpScore(info, combined) },
            infer,
        )
        val runnerUps = ranked.map { it.mWord?.toString().orEmpty() }
        HeatmapSwipeDecodeSnapshot_v1.stash(infer.pathLettersRaw, infer.pathLetters, runnerUps)
        if (ranked.isNotEmpty()) {
            Log.i(TAG, "top=${ranked.first().mWord} runners=${runnerUps.take(5)} prefixes=${prefixes.take(4)}")
        }
        val result = DecodeResult(ranked = ranked, infer = infer)
        if (isPreview) {
            previewCacheKey = cacheKey
            previewCache = result
        }
        return result
    }

    private fun bumpScore(info: SuggestedWordInfo, combined: Double): SuggestedWordInfo {
        if (info.mSourceDict == null) return info
        val boosted = (combined * 900_000).toInt().coerceAtLeast(1)
        return SuggestedWordInfo(
            info.mWord,
            info.mPrevWordsContext,
            boosted,
            info.mKindAndFlags,
            info.mSourceDict,
            info.mIndexOfTouchPointOfSecondWord,
            info.mAutoCommitFirstWordConfidence,
        )
    }
}
