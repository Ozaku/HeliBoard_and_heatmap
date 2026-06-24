// SPDX-License-Identifier: GPL-3.0-only

// ai-note: Block 3 step 15t — dict fetch only on start-anchored prefixes; v13 scorer

package helium314.keyboard.heatmap.swipe

import helium314.keyboard.heatmap.learning.HeatmapCoordinateMap_v1
import helium314.keyboard.keyboard.Keyboard
import helium314.keyboard.latin.DictionaryFacilitator
import helium314.keyboard.latin.NgramContext
import helium314.keyboard.latin.SuggestedWords
import helium314.keyboard.latin.SuggestedWords.SuggestedWordInfo
import helium314.keyboard.latin.common.ComposedData
import helium314.keyboard.latin.settings.SettingsValuesForSuggestion
import helium314.keyboard.latin.utils.SuggestionResults

object HeatmapSwipeCandidateSource_v21 {

    private const val SESSION_ID_TYPING = 0
    private const val GEO_WEIGHT = 0.93
    private const val DICT_WEIGHT = 0.07
    private const val LIGHT_MAX_PREFIXES = 10
    private const val LIGHT_MAX_RESULTS = 16

    fun collectDictionaryCandidates(
        prefixes: List<String>,
        infer: HeatmapSwipeSegmentInfer_v18.Result,
        facilitator: DictionaryFacilitator,
        ngramContext: NgramContext,
        keyboard: Keyboard,
        settings: SettingsValuesForSuggestion,
        inputStyle: Int,
        maxResults: Int = 32,
    ): List<Pair<SuggestedWordInfo, Double>> {
        val inferV12 = HeatmapSwipeInferCompat_v6.toV12(infer)
        val neighborGraph = HeatmapCoordinateMap_v1.fromKeyboard(keyboard)
            ?.let { HeatmapKeyNeighborGraph_v2.fromLayout(it) }
        val lightPreview = inputStyle == SuggestedWords.INPUT_STYLE_UPDATE_BATCH
        val seen = HashSet<String>()
        val scored = ArrayList<Pair<SuggestedWordInfo, Double>>()
        val maxDictLen = infer.maxWordLength
        val requireEnd = HeatmapSwipeEndLetterPolicy_v2.requiresEndMatch(inferV12)
        val minWordLen = HeatmapSwipeMaxWordLenPolicy_v3.minOutputLen(
            infer.touchedLetters.size,
            infer.pathLetters.size,
            infer.beatCount,
            infer.beatCountRaw,
        )
        val start = infer.startKeyLabel

        val pathVariants = HeatmapSwipePathNeighborExpand_v11.expand(
            inferV12, maxDictLen, infer.doublePrefixIndices,
        )
        val neighborPrefixes = HeatmapSwipePathNeighborExpand_v11.prefixStrings(pathVariants, maxDictLen)
        val allPrefixes = LinkedHashSet<String>()
        allPrefixes.addAll(prefixes)
        allPrefixes.addAll(neighborPrefixes)
        val orderedPrefixes = HeatmapSwipeStartLetterAnchor_v1.filterPrefixes(allPrefixes, start)
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
                    scored, seen, info, inferV12, facilitator, minWordLen, maxDictLen,
                    requireEnd, neighborGraph,
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
        infer: HeatmapSwipeSegmentInfer_v12.Result,
        facilitator: DictionaryFacilitator,
        minWordLen: Int,
        maxDictLen: Int,
        requireEnd: Boolean,
        neighborGraph: HeatmapKeyNeighborGraph_v2.Graph?,
    ) {
        val word = info.mWord?.toString() ?: return
        if (!HeatmapSwipeWordTouchGate_v2.isAllowed(
                word, infer.touchedLetters, infer.startKeyLabel, emptySet(),
            )
        ) {
            return
        }
        if (!HeatmapSwipeStartLetterAnchor_v1.wordStartsWithAnchor(word, infer.startKeyLabel)) {
            return
        }
        if (requireEnd && !HeatmapSwipeEndLetterPolicy_v2.wordEndsOnLift(word, infer.endKeyLabel)) {
            return
        }
        val key = word.lowercase()
        if (HeatmapSwipeSlotRejectMemory_v1.isRejected(
                infer.pathLetters, infer.startKeyLabel, infer.endKeyLabel, word,
            )
        ) {
            return
        }
        val lettersOnly = HeatmapSwipeContractionExpand_v1.lettersOnly(word)
        if (lettersOnly.length < minWordLen) return
        val dictLen = if (word.contains('\'')) lettersOnly.length else word.length
        if (dictLen > maxDictLen && !word.contains('\'')) return
        if (!facilitator.isValidSpellingWord(word) && lettersOnly.length <= 2) {
            if (lettersOnly != infer.pathLetters.joinToString("")) return
        } else if (!facilitator.isValidSpellingWord(word)) {
            return
        }
        if (!seen.add(key)) return
        val geo = HeatmapLiteralSwipeScore_v13.scoreWord(
            candidate = word,
            pathLetters = infer.pathLetters,
            touchedLetters = infer.touchedLetters,
            startLabel = infer.startKeyLabel,
            endLabel = infer.endKeyLabel,
            requireEndMatch = requireEnd,
            neighborGraph = neighborGraph,
        )
        if (geo <= 0.0) return
        val dictNorm = normalizeDictScore(info.mScore)
        val lengthMul = HeatmapSwipeLengthBand_v2.multiplier(lettersOnly.length, infer.pathLetters.size)
        val combined = (geo * GEO_WEIGHT + dictNorm * DICT_WEIGHT) * lengthMul
        if (combined <= 0.0) return
        scored.add(info to combined)
    }

    private fun normalizeDictScore(raw: Int): Double {
        if (raw <= 0) return 0.0
        return (raw.coerceAtMost(500_000) / 500_000.0).coerceIn(0.0, 1.0)
    }
}
