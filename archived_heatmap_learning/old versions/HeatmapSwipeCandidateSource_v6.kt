// SPDX-License-Identifier: GPL-3.0-only

// ai-note: Block 3 step 15e — 2-key straight: end gate, literal+contraction synthetics, no is-from-i prefix

package helium314.keyboard.heatmap.swipe

import helium314.keyboard.keyboard.Keyboard
import helium314.keyboard.latin.dictionary.Dictionary
import helium314.keyboard.latin.DictionaryFacilitator
import helium314.keyboard.latin.NgramContext
import helium314.keyboard.latin.SuggestedWords.SuggestedWordInfo
import helium314.keyboard.latin.common.ComposedData
import helium314.keyboard.latin.settings.SettingsValuesForSuggestion
import helium314.keyboard.latin.utils.SuggestionResults

object HeatmapSwipeCandidateSource_v6 {

    private const val SESSION_ID_TYPING = 0
    private const val GEO_WEIGHT = 0.72
    private const val DICT_WEIGHT = 0.28
    private const val LITERAL_PATH_BOOST = 0.88
    private const val CONTRACTION_BOOST = 0.98

    fun fetchCompletionsForPrefix(
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

    fun collectDictionaryCandidates(
        prefixes: List<String>,
        infer: HeatmapSwipeSegmentInfer_v4.Result,
        facilitator: DictionaryFacilitator,
        ngramContext: NgramContext,
        keyboard: Keyboard,
        settings: SettingsValuesForSuggestion,
        inputStyle: Int,
        maxResults: Int = 32,
    ): List<Pair<SuggestedWordInfo, Double>> {
        val seen = HashSet<String>()
        val scored = ArrayList<Pair<SuggestedWordInfo, Double>>()
        val pathJoin = infer.pathLetters.joinToString("")
        val strictTwoKey = infer.straightLine.locksLetterCount && infer.pathLetters.size >= 2
        val strictEnd = strictTwoKey
        val minWordLen = if (strictTwoKey) 2 else 1
        val maxDictLen = infer.maxWordLength
        val maxDisplayLen = if (strictTwoKey) 8 else maxDictLen

        for (prefix in prefixes) {
            if (prefix.isEmpty()) continue
            val results = fetchCompletionsForPrefix(
                prefix, facilitator, ngramContext, keyboard, settings, inputStyle,
            )
            for (info in results) {
                addScoredCandidate(
                    scored, seen, info, infer, facilitator, strictEnd, minWordLen, maxDictLen,
                )
            }
            if (scored.size >= maxResults) break
        }

        if (strictTwoKey && pathJoin.length >= 2) {
            addSynthetic(scored, seen, pathJoin, pathJoin, LITERAL_PATH_BOOST, infer, strictEnd)
            for (contraction in HeatmapSwipeContractionExpand_v1.expansions(pathJoin)) {
                if (contraction.length <= maxDisplayLen) {
                    addSynthetic(scored, seen, contraction, pathJoin, CONTRACTION_BOOST, infer, strictEnd)
                }
            }
        }

        return scored.sortedByDescending { it.second }.take(maxResults)
    }

    private fun addScoredCandidate(
        scored: MutableList<Pair<SuggestedWordInfo, Double>>,
        seen: MutableSet<String>,
        info: SuggestedWordInfo,
        infer: HeatmapSwipeSegmentInfer_v4.Result,
        facilitator: DictionaryFacilitator,
        strictEnd: Boolean,
        minWordLen: Int,
        maxDictLen: Int,
    ) {
        val word = info.mWord?.toString() ?: return
        val key = word.lowercase()
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
        val geo = HeatmapLiteralSwipeScore_v4.scoreWord(
            candidate = word,
            pathLetters = infer.pathLetters,
            startLabel = infer.startKeyLabel,
            endLabel = infer.endKeyLabel,
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
        pathJoin: String,
        boost: Double,
        infer: HeatmapSwipeSegmentInfer_v4.Result,
        strictEnd: Boolean,
    ) {
        val key = displayWord.lowercase()
        if (!seen.add(key)) return
        val geo = HeatmapLiteralSwipeScore_v4.scoreWord(
            candidate = displayWord,
            pathLetters = infer.pathLetters,
            startLabel = infer.startKeyLabel,
            endLabel = infer.endKeyLabel,
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
        if (delta == 1 && geo >= 0.82) return base.coerceAtLeast(0.92)
        return base
    }

    private fun normalizeDictScore(raw: Int): Double {
        if (raw <= 0) return 0.0
        return (raw.coerceAtMost(500_000) / 500_000.0).coerceIn(0.0, 1.0)
    }
}
