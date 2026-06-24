// SPDX-License-Identifier: GPL-3.0-only

// ai-note: Block 3 step 15g — neighbor near-match finalize wired into decode pipeline

package helium314.keyboard.heatmap.swipe

import helium314.keyboard.keyboard.Keyboard
import helium314.keyboard.latin.DictionaryFacilitator
import helium314.keyboard.latin.NgramContext
import helium314.keyboard.latin.SuggestedWords.SuggestedWordInfo
import helium314.keyboard.latin.common.InputPointers
import helium314.keyboard.latin.settings.SettingsValuesForSuggestion
import helium314.keyboard.latin.utils.Log

object HeatmapLiteralSwipeDecode_v8 {

    private const val TAG = "HeatmapLiteralSwipe"

    data class DecodeResult(
        val ranked: List<SuggestedWordInfo>,
        val infer: HeatmapSwipeSegmentInfer_v5.Result?,
    )

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
        val infer = HeatmapSwipeSegmentInfer_v5.infer(keyboard, pointers)
        if (infer == null || infer.pathLetters.isEmpty()) {
            Log.i(TAG, "decode skip — no geometry inference")
            return DecodeResult(ranked = emptyList(), infer = null)
        }
        val pathEnd = infer.pathLetters.lastOrNull()
        val liftNote = if (pathEnd != infer.endKeyLabel) " liftEnd=${infer.endKeyLabel}" else ""
        val neighborCount = HeatmapSwipePathNeighborExpand_v1.expand(
            infer.pathLetters, infer.pathLettersRaw, null,
        ).size
        Log.i(
            TAG,
            "shape=${infer.straightLine.shape} maxLen=${infer.maxWordLength} " +
                "beatsRaw=${infer.beatCountRaw} beats=${infer.beatCount} " +
                "pathRaw=${infer.pathLettersRaw.joinToString("")} " +
                "path=${infer.pathLetters.joinToString("")} " +
                "start=${infer.startKeyLabel} end=${infer.endKeyLabel}$liftNote " +
                "neighborPaths=$neighborCount",
        )
        val prefixes = HeatmapSwipePrefixEngine_v6.buildPrefixVariants(infer)
        val scored = HeatmapSwipeCandidateSource_v8.collectDictionaryCandidates(
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
        val ranked = scored.map { (info, combined) -> bumpScore(info, combined) }
        val runnerUps = scored.map { it.first.mWord?.toString().orEmpty() }
        HeatmapSwipeDecodeSnapshot_v1.stash(infer.pathLettersRaw, infer.pathLetters, runnerUps)
        if (ranked.isNotEmpty()) {
            Log.i(TAG, "top=${ranked.first().mWord} runners=${runnerUps.take(5)} prefixes=$prefixes")
        }
        return DecodeResult(ranked = ranked, infer = infer)
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
