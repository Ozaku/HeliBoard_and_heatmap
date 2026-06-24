// SPDX-License-Identifier: GPL-3.0-only

// ai-note: v24 — v16 intent scorer + v20 prefixes + v3 end policy + v3 path capture/snapshot

package helium314.keyboard.heatmap.swipe

import helium314.keyboard.heatmap.learning.HeatmapCoordinateMap_v1
import helium314.keyboard.heatmap.learning.HeatmapPathCapture_v3
import helium314.keyboard.keyboard.Keyboard
import helium314.keyboard.latin.DictionaryFacilitator
import helium314.keyboard.latin.NgramContext
import helium314.keyboard.latin.SuggestedWords
import helium314.keyboard.latin.SuggestedWords.SuggestedWordInfo
import helium314.keyboard.latin.common.ComposedData
import helium314.keyboard.latin.common.InputPointers
import helium314.keyboard.latin.settings.SettingsValuesForSuggestion
import helium314.keyboard.latin.utils.SuggestionResults

object HeatmapSwipeCandidateSource_v24 {

    private const val SESSION_ID_TYPING = 0
    private const val GEO_WEIGHT = 0.93
    private const val DICT_WEIGHT = 0.07
    private const val LIGHT_MAX_PREFIXES = 14
    private const val LIGHT_MAX_RESULTS = 20

    fun collectDictionaryCandidates(
        prefixes: List<String>,
        infer: HeatmapSwipeSegmentInfer_v19.Result,
        facilitator: DictionaryFacilitator,
        ngramContext: NgramContext,
        keyboard: Keyboard,
        settings: SettingsValuesForSuggestion,
        inputStyle: Int,
        maxResults: Int = 32,
    ): List<Pair<SuggestedWordInfo, Double>> {
        val inferV12 = HeatmapSwipeInferCompat_v8.intentPrimaryV12(infer)
        val neighborGraph = HeatmapCoordinateMap_v1.fromKeyboard(keyboard)
            ?.let { HeatmapKeyNeighborGraph_v2.fromLayout(it) }
        val lightPreview = inputStyle == SuggestedWords.INPUT_STYLE_UPDATE_BATCH
        val seen = HashSet<String>()
        val scored = ArrayList<Pair<SuggestedWordInfo, Double>>()
        val maxDictLen = infer.maxWordLength
        val requireEnd = HeatmapSwipeEndLetterPolicy_v3.requiresEndMatch(infer)
        val intentLen = infer.intentPathLetters.size
        val pathLen = if (intentLen > 0) intentLen else infer.pathLetters.size
        val minWordLen = HeatmapSwipeMaxWordLenPolicy_v3.minOutputLen(
            infer.touchedLetters.size,
            pathLen,
            infer.beatCount,
            infer.beatCountRaw,
        )
        val visitLen = infer.strokeOrderLetters.size

        val pathVariants = HeatmapSwipePathNeighborExpand_v12.expand(
            infer, maxDictLen, infer.doublePrefixIndices,
        )
        val neighborPrefixes = HeatmapSwipePathNeighborExpand_v12.prefixStrings(pathVariants, maxDictLen)
        val allPrefixes = LinkedHashSet<String>()
        allPrefixes.addAll(prefixes)
        allPrefixes.addAll(neighborPrefixes)
        val orderedPrefixes = HeatmapSwipeStartLetterSoftAnchor_v1.filterPrefixes(
            allPrefixes, infer.startDistribution, neighborGraph,
        )
            .sortedByDescending { it.length }
            .let { if (lightPreview) it.take(LIGHT_MAX_PREFIXES) else it }
        val resultCap = if (lightPreview) LIGHT_MAX_RESULTS else maxResults

        for (prefix in orderedPrefixes) {
            if (prefix.isEmpty()) continue
            val results = fetchCompletionsForPrefix(
                prefix, facilitator, ngramContext, keyboard, settings, inputStyle,
            )
            for (info in results) {
                addScoredCandidate(
                    scored, seen, info, infer, inferV12, facilitator, minWordLen, maxDictLen,
                    requireEnd, neighborGraph, visitLen, pathLen,
                )
            }
            if (scored.size >= resultCap) break
        }

        return scored.sortedByDescending { it.second }.take(resultCap)
    }

    private fun fetchCompletionsForPrefix(
        prefix: String,
        facilitator: DictionaryFacilitator,
        ngramContext: NgramContext,
        keyboard: Keyboard,
        settings: SettingsValuesForSuggestion,
        inputStyle: Int,
    ): SuggestionResults {
        val composed = ComposedData.createForWord(prefix)
        return facilitator.getSuggestionResults(
            composed, ngramContext, keyboard, settings, SESSION_ID_TYPING, inputStyle,
        )
    }

    private fun addScoredCandidate(
        scored: MutableList<Pair<SuggestedWordInfo, Double>>,
        seen: MutableSet<String>,
        info: SuggestedWordInfo,
        infer: HeatmapSwipeSegmentInfer_v19.Result,
        inferV12: HeatmapSwipeSegmentInfer_v12.Result,
        facilitator: DictionaryFacilitator,
        minWordLen: Int,
        maxDictLen: Int,
        requireEnd: Boolean,
        neighborGraph: HeatmapKeyNeighborGraph_v2.Graph?,
        visitLen: Int,
        pathLen: Int,
    ) {
        val word = info.mWord?.toString() ?: return
        if (!HeatmapSwipeWordTouchGate_v2.isAllowed(
                word, infer.touchedLetters, infer.startKeyLabel, emptySet(),
            )
        ) {
            return
        }
        if (!HeatmapSwipeStartLetterSoftAnchor_v1.wordAllowedAtStart(
                word, infer.startDistribution, neighborGraph,
            )
        ) {
            return
        }
        if (requireEnd && !HeatmapSwipeEndLetterPolicy_v3.wordEndsOnLift(word, infer.endKeyLabel, neighborGraph)) {
            return
        }
        val rejectPath = infer.intentPathLetters.ifEmpty { infer.pathLetters }
        val key = word.lowercase()
        if (HeatmapSwipeSlotRejectMemory_v1.isRejected(
                rejectPath, infer.startKeyLabel, infer.endKeyLabel, word,
            )
        ) {
            return
        }
        val lettersOnly = HeatmapSwipeContractionExpand_v1.lettersOnly(word)
        if (lettersOnly.length < minWordLen) return
        val dictLen = if (word.contains('\'')) lettersOnly.length else word.length
        if (dictLen > maxDictLen && !word.contains('\'')) return
        val intentStr = infer.intentPathLetters.joinToString("")
        if (!facilitator.isValidSpellingWord(word) && lettersOnly.length <= 2) {
            if (lettersOnly != intentStr && lettersOnly != infer.pathLetters.joinToString("")) return
        } else if (!facilitator.isValidSpellingWord(word)) {
            return
        }
        if (!seen.add(key)) return
        val geo = HeatmapLiteralSwipeScore_v16.scoreWord(
            candidate = word,
            pathLetters = infer.pathLetters,
            intentPathLetters = infer.intentPathLetters,
            touchedLetters = infer.touchedLetters,
            startLabel = infer.startKeyLabel,
            startDistribution = infer.startDistribution,
            endLabel = infer.endKeyLabel,
            requireEndMatch = requireEnd,
            neighborGraph = neighborGraph,
            dwellHints = infer.normalized.dwellHints,
            kinematics = infer.kinematics,
        )
        if (geo <= 0.0) return
        val dictNorm = normalizeDictScore(info.mScore)
        val lengthMul = HeatmapSwipeLengthBand_v3.multiplier(
            lettersOnly.length, pathLen, visitLen,
        )
        val combined = (geo * GEO_WEIGHT + dictNorm * DICT_WEIGHT) * lengthMul
        if (combined <= 0.0) return
        scored.add(info to combined)
    }

    private fun normalizeDictScore(raw: Int): Double {
        if (raw <= 0) return 0.0
        return (raw.coerceAtMost(500_000) / 500_000.0).coerceIn(0.0, 1.0)
    }
}
