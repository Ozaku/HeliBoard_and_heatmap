// SPDX-License-Identifier: GPL-3.0-only

// ai-note: v25 — ordered corner path; tuning revision 4; cross-thread snapshot + path capture

package helium314.keyboard.heatmap.swipe

import helium314.keyboard.heatmap.learning.HeatmapPathCapture_v3
import helium314.keyboard.keyboard.Keyboard
import helium314.keyboard.latin.DictionaryFacilitator
import helium314.keyboard.latin.NgramContext
import helium314.keyboard.latin.SuggestedWords
import helium314.keyboard.latin.SuggestedWords.SuggestedWordInfo
import helium314.keyboard.latin.common.InputPointers
import helium314.keyboard.latin.settings.SettingsValuesForSuggestion
import helium314.keyboard.latin.utils.Log

object HeatmapLiteralSwipeDecode_v25 {

    private const val TAG = "HeatmapLiteralSwipe"

    data class DecodeResult(
        val ranked: List<SuggestedWordInfo>,
        val infer: HeatmapSwipeSegmentInfer_v19.Result?,
        val diagnostics: HeatmapSwipeDecodeDiagnostics_v1.Bundle?,
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
            return DecodeResult(ranked = emptyList(), infer = null, diagnostics = null)
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
        val decodePointCount = pointers.pointerSize
        val stashPointCount = HeatmapPathCapture_v3.peekPointerSize()
        if (stashPointCount > 0 && decodePointCount != stashPointCount) {
            Log.w(
                TAG,
                "pointerIntegrity mismatch decode=$decodePointCount stash=$stashPointCount",
            )
        }
        val infer = HeatmapSwipeSegmentInfer_v21.infer(keyboard, pointers)
        if (infer == null ||
            (infer.intentPathLetters.isEmpty())
        ) {
            Log.i(TAG, "decode skip — no geometry inference")
            return DecodeResult(ranked = emptyList(), infer = null, diagnostics = null)
        }
        val inferV12 = HeatmapSwipeInferCompat_v8.intentPrimaryV12(infer)
        val touched = infer.touchedLetters.joinToString("")
        val touchCounts = infer.touchCounts.entries.joinToString(",") { "${it.key}:${it.value}" }
        val rejected = infer.rejectedTouchLetters.joinToString("")
        val strokeOrder = infer.strokeOrderLetters.joinToString("")
        val transit = infer.transitKeys.joinToString("")
        val startDist = infer.startDistribution.joinToString(",") {
            "${it.label}:${"%.2f".format(it.likelihood)}"
        }
        val doubleHints = infer.doublePrefixIndices.joinToString(",")
        val requireEnd = HeatmapSwipeEndLetterPolicy_v3.requiresEndMatch(infer)
        val startAnchor = infer.startKeyLabel.orEmpty()
        val cacheKey = infer.intentPathLetters.joinToString("") + "|" + infer.pathLetters.joinToString("") +
            "|" + infer.endKeyLabel + "|" + touched +
            "|" + infer.beatCountRaw + "|" + doubleHints + "|" + startAnchor + "|" + startDist
        if (isPreview && previewCacheKey == cacheKey && previewCache != null) {
            return previewCache!!
        }
        if (isTail) {
            previewCacheKey = null
            previewCache = null
        }
        val graph = helium314.keyboard.heatmap.learning.HeatmapCoordinateMap_v1.fromKeyboard(keyboard)
            ?.let { HeatmapKeyNeighborGraph_v2.fromLayout(it) }
        val prefixes = HeatmapSwipePrefixEngine_v21.buildPrefixVariants(
            infer, infer.doublePrefixIndices, graph,
        )
        val mode = if (isPreview) "preview" else "full"
        Log.i(
            TAG,
            "mode=$mode tuning=${HeatmapSwipeTuningConstants_v4.TUNING_REVISION} " +
                "pathLen=${infer.pathLetters.size} strokeLen=${infer.strokeOrderLetters.size} " +
                "intentLen=${infer.intentPathLetters.size} slow=${infer.kinematics.isSlowStroke} " +
                "avgSpeed=${"%.2f".format(infer.kinematics.avgSpeedKeyWidthsPerSec)}kw/s " +
                "doubleHints=[$doubleHints] shape=${infer.straightLine.shape} beatsRaw=${infer.beatCountRaw} " +
                "beats=${infer.beatCount} maxLen=${infer.maxWordLength} requireEnd=$requireEnd " +
                "pathRaw=${infer.pathLettersRaw.joinToString("")} path=${infer.pathLetters.joinToString("")} " +
                "intentPath=${infer.intentPathLetters.joinToString("")} strokeOrder=[$strokeOrder] " +
                "transit=[$transit] touched=[$touched] touchCounts=[$touchCounts] rejected=[$rejected] " +
                "startAnchor=$startAnchor startDist=[$startDist] end=${infer.endKeyLabel} " +
                "points=$decodePointCount stash=$stashPointCount",
        )
        val scored = HeatmapSwipeCandidateSource_v25.collectDictionaryCandidates(
            prefixes = prefixes,
            infer = infer,
            facilitator = facilitator,
            ngramContext = ngramContext,
            keyboard = keyboard,
            settings = settings,
            inputStyle = inputStyle,
        )
        val diagnostics = HeatmapSwipeDecodeDiagnostics_v1.build(
            infer = infer,
            kinematics = infer.kinematics,
            intent = infer.intent,
            prefixes = prefixes,
            scored = scored,
            decodePointCount = decodePointCount,
            stashPointCount = stashPointCount,
        )
        if (scored.isEmpty()) {
            Log.i(TAG, "decode — no dictionary matches for prefixes=$prefixes")
            HeatmapSwipeDecodeSnapshot_v3.stash(infer, emptyList(), diagnostics)
            return DecodeResult(ranked = emptyList(), infer = infer, diagnostics = diagnostics)
        }
        val bumped = scored.map { (info, combined) -> bumpScore(info, combined) }
        val rejectFiltered = HeatmapSwipeSlotRejectMemory_v6.filterRanked(bumped, inferV12)
        val ranked = HeatmapSwipeMinWordLenGuard_v10.filterRanked(rejectFiltered, infer)
        val runnerUps = ranked.map { it.mWord?.toString().orEmpty() }
        HeatmapSwipeDecodeSnapshot_v3.stash(infer, runnerUps, diagnostics)
        if (ranked.isNotEmpty()) {
            Log.i(TAG, "top=${ranked.first().mWord} runners=${runnerUps.take(5)} prefixes=${prefixes.take(5)}")
        }
        val result = DecodeResult(ranked = ranked, infer = infer, diagnostics = diagnostics)
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
