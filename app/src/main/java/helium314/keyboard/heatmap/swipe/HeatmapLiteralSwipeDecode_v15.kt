// SPDX-License-Identifier: GPL-3.0-only

// ai-note: Block 3 step 15n — strict lift-end; logs end= label

package helium314.keyboard.heatmap.swipe

import helium314.keyboard.keyboard.Keyboard
import helium314.keyboard.latin.DictionaryFacilitator
import helium314.keyboard.latin.NgramContext
import helium314.keyboard.latin.SuggestedWords
import helium314.keyboard.latin.SuggestedWords.SuggestedWordInfo
import helium314.keyboard.latin.common.InputPointers
import helium314.keyboard.latin.settings.SettingsValuesForSuggestion
import helium314.keyboard.latin.utils.Log

object HeatmapLiteralSwipeDecode_v15 {

    private const val TAG = "HeatmapLiteralSwipe"

    data class DecodeResult(
        val ranked: List<SuggestedWordInfo>,
        val infer: HeatmapSwipeSegmentInfer_v12.Result?,
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
        val infer = HeatmapSwipeSegmentInfer_v12.infer(keyboard, pointers)
        if (infer == null || infer.pathLetters.isEmpty()) {
            Log.i(TAG, "decode skip — no geometry inference")
            return DecodeResult(ranked = emptyList(), infer = null)
        }
        val touched = infer.touchedLetters.joinToString("")
        val touchCounts = infer.touchCounts.entries.joinToString(",") { "${it.key}:${it.value}" }
        val rejected = infer.rejectedTouchLetters.joinToString("")
        val strokeOrder = infer.strokeOrderLetters.joinToString("")
        val requireEnd = HeatmapSwipeEndLetterPolicy_v1.requiresEndMatch(infer)
        val cacheKey = infer.pathLetters.joinToString("") + "|" + infer.endKeyLabel + "|" + touched
        if (isPreview && previewCacheKey == cacheKey && previewCache != null) {
            return previewCache!!
        }
        if (isTail) {
            previewCacheKey = null
            previewCache = null
        }
        val mode = if (isPreview) "preview" else "full"
        Log.i(
            TAG,
            "mode=$mode shape=${infer.straightLine.shape} beatsRaw=${infer.beatCountRaw} beats=${infer.beatCount} " +
                "maxLen=${infer.maxWordLength} requireEnd=$requireEnd pathRaw=${infer.pathLettersRaw.joinToString("")} " +
                "path=${infer.pathLetters.joinToString("")} strokeOrder=[$strokeOrder] touched=[$touched] " +
                "touchCounts=[$touchCounts] rejected=[$rejected] start=${infer.startKeyLabel} end=${infer.endKeyLabel}",
        )
        val prefixes = HeatmapSwipePrefixEngine_v13.buildPrefixVariants(infer)
        val scored = HeatmapSwipeCandidateSource_v15.collectDictionaryCandidates(
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
        val bumped = scored.map { (info, combined) -> bumpScore(info, combined) }
        val rejectFiltered = HeatmapSwipeSlotRejectMemory_v6.filterRanked(bumped, infer)
        val ranked = HeatmapSwipeMinWordLenGuard_v2.filterRanked(rejectFiltered, infer)
        val runnerUps = ranked.map { it.mWord?.toString().orEmpty() }
        HeatmapSwipeDecodeSnapshot_v1.stash(infer.pathLettersRaw, infer.pathLetters, runnerUps)
        if (ranked.isNotEmpty()) {
            Log.i(TAG, "top=${ranked.first().mWord} runners=${runnerUps.take(5)} prefixes=${prefixes.take(5)}")
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
