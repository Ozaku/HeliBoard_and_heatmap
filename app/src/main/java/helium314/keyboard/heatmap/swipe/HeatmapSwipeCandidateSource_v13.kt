// SPDX-License-Identifier: GPL-3.0-only

// ai-note: Block 3 step 15l — word touch gate on every candidate; infer v10

package helium314.keyboard.heatmap.swipe

import helium314.keyboard.heatmap.learning.HeatmapCoordinateMap_v1
import helium314.keyboard.keyboard.Keyboard
import helium314.keyboard.latin.DictionaryFacilitator
import helium314.keyboard.latin.NgramContext
import helium314.keyboard.latin.SuggestedWords
import helium314.keyboard.latin.SuggestedWords.SuggestedWordInfo
import helium314.keyboard.latin.common.ComposedData
import helium314.keyboard.latin.dictionary.Dictionary
import helium314.keyboard.latin.settings.SettingsValuesForSuggestion
import helium314.keyboard.latin.utils.SuggestionResults

object HeatmapSwipeCandidateSource_v13 {

    private const val SESSION_ID_TYPING = 0
    private const val GEO_WEIGHT = 0.95
    private const val DICT_WEIGHT = 0.05
    private const val LITERAL_PATH_BOOST = 0.9
    private const val CONTRACTION_BOOST = 1.0
    private const val GENERAL_MAX_LEN_SLACK = 2
    private const val GENERAL_MAX_LEN_CAP = 20
    private const val LIGHT_MAX_PREFIXES = 8
    private const val LIGHT_MAX_RESULTS = 14

    fun collectDictionaryCandidates(
        prefixes: List<String>,
        infer: HeatmapSwipeSegmentInfer_v10.Result,
        facilitator: DictionaryFacilitator,
        ngramContext: NgramContext,
        keyboard: Keyboard,
        settings: SettingsValuesForSuggestion,
        inputStyle: Int,
        maxResults: Int = 32,
    ): List<Pair<SuggestedWordInfo, Double>> {
        val dwellDoubles = HeatmapSwipeDwellDoubleLetter_v1.dwellDoubleChars(infer.normalized)
        val lightPreview = inputStyle == SuggestedWords.INPUT_STYLE_UPDATE_BATCH
        val seen = HashSet<String>()
        val scored = ArrayList<Pair<SuggestedWordInfo, Double>>()
        val pathJoin = infer.pathLetters.joinToString("")
        val strictTwoKey = infer.straightLine.locksLetterCount && infer.pathLetters.size >= 2
        val strictEnd = strictTwoKey
        val minWordLen = if (strictTwoKey) 2 else 1
        val maxDictLen = resolveMaxDictLen(infer, strictTwoKey)
        val maxDisplayLen = if (strictTwoKey) 8 else maxDictLen

        val layout = HeatmapCoordinateMap_v1.fromKeyboard(keyboard)
        val pathVariants = HeatmapSwipePathNeighborExpand_v6.expand(infer, layout, lightPreview)
        val neighborPrefixes = HeatmapSwipePathNeighborExpand_v6.prefixStrings(pathVariants, maxDictLen)
        val allPrefixes = LinkedHashSet<String>()
        allPrefixes.addAll(prefixes)
        allPrefixes.addAll(neighborPrefixes)
        val orderedPrefixes = allPrefixes.sortedByDescending { it.length }
            .let { if (lightPreview) it.take(LIGHT_MAX_PREFIXES) else it }
        val resultCap = if (lightPreview) LIGHT_MAX_RESULTS else maxResults

        for (prefix in orderedPrefixes) {
            if (prefix.isEmpty()) continue
            val results = fetchCompletionsForPrefix(
                prefix, facilitator, ngramContext, keyboard, settings, inputStyle,
            )
            for (info in results) {
                addScoredCandidate(
                    scored, seen, info, infer, facilitator,
                    dwellDoubles, strictEnd, minWordLen, maxDictLen,
                )
            }
            if (scored.size >= resultCap) break
        }

        if (strictTwoKey && pathJoin.length >= 2 && !lightPreview) {
            addSynthetic(scored, seen, pathJoin, infer, dwellDoubles, LITERAL_PATH_BOOST, strictEnd)
            for (contraction in HeatmapSwipeContractionExpand_v1.expansions(pathJoin)) {
                if (contraction.length <= maxDisplayLen) {
                    addSynthetic(scored, seen, contraction, infer, dwellDoubles, CONTRACTION_BOOST, strictEnd)
                }
            }
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

    private fun resolveMaxDictLen(infer: HeatmapSwipeSegmentInfer_v10.Result, strictTwoKey: Boolean): Int {
        if (strictTwoKey) return infer.maxWordLength
        if (infer.straightLine.locksLetterCount) return infer.maxWordLength
        return (infer.beatCount + GENERAL_MAX_LEN_SLACK).coerceIn(2, GENERAL_MAX_LEN_CAP)
    }

    private fun addScoredCandidate(
        scored: MutableList<Pair<SuggestedWordInfo, Double>>,
        seen: MutableSet<String>,
        info: SuggestedWordInfo,
        infer: HeatmapSwipeSegmentInfer_v10.Result,
        facilitator: DictionaryFacilitator,
        dwellDoubles: Set<Char>,
        strictEnd: Boolean,
        minWordLen: Int,
        maxDictLen: Int,
    ) {
        val word = info.mWord?.toString() ?: return
        if (!HeatmapSwipeWordTouchGate_v1.isAllowed(word, infer.touchedLetters, dwellDoubles)) {
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
        val geo = HeatmapLiteralSwipeScore_v10.scoreWord(
            candidate = word,
            pathLetters = infer.pathLetters,
            touchedLetters = infer.touchedLetters,
            startLabel = infer.startKeyLabel,
            endLabel = infer.endKeyLabel,
            dwellDoubleLetters = dwellDoubles,
            strictEndMatch = strictEnd,
        )
        if (geo <= 0.0) return
        val dictNorm = normalizeDictScore(info.mScore)
        val lengthMul = lengthMultiplier(lettersOnly.length, infer.beatCount, geo, infer.straightLine.locksLetterCount)
        val combined = (geo * GEO_WEIGHT + dictNorm * DICT_WEIGHT) * lengthMul
        if (combined <= 0.0) return
        scored.add(info to combined)
    }

    private fun addSynthetic(
        scored: MutableList<Pair<SuggestedWordInfo, Double>>,
        seen: MutableSet<String>,
        displayWord: String,
        infer: HeatmapSwipeSegmentInfer_v10.Result,
        dwellDoubles: Set<Char>,
        boost: Double,
        strictEnd: Boolean,
    ) {
        val key = displayWord.lowercase()
        if (!seen.add(key)) return
        val geo = HeatmapLiteralSwipeScore_v10.scoreWord(
            candidate = displayWord,
            pathLetters = infer.pathLetters,
            touchedLetters = infer.touchedLetters,
            startLabel = infer.startKeyLabel,
            endLabel = infer.endKeyLabel,
            dwellDoubleLetters = dwellDoubles,
            strictEndMatch = strictEnd,
        )
        if (geo <= 0.0) return
        val combined = (geo * GEO_WEIGHT + boost * DICT_WEIGHT) * boost
        val info = SuggestedWordInfo(
            displayWord,
            "",
            (combined * 900_000).toInt().coerceAtLeast(1),
            SuggestedWordInfo.KIND_TYPED,
            Dictionary.DICTIONARY_USER_TYPED,
            SuggestedWordInfo.NOT_AN_INDEX,
            SuggestedWordInfo.NOT_A_CONFIDENCE,
        )
        scored.add(info to combined)
    }

    private fun lengthMultiplier(
        candidateLen: Int,
        beatCount: Int,
        geo: Double,
        straightLocked: Boolean,
    ): Double {
        val base = HeatmapSwipeLengthBand_v1.multiplier(candidateLen, beatCount)
        if (straightLocked) return base
        val delta = kotlin.math.abs(candidateLen - beatCount)
        if (delta == 1 && geo >= 0.72) return base.coerceAtLeast(0.96)
        if (delta == 2 && geo >= 0.78) return base.coerceAtLeast(0.92)
        return base
    }

    private fun normalizeDictScore(raw: Int): Double {
        if (raw <= 0) return 0.0
        return (raw.coerceAtMost(500_000) / 500_000.0).coerceIn(0.0, 1.0)
    }
}
