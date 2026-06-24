// SPDX-License-Identifier: GPL-3.0-only

// ai-note: Block 3 step 15c — v3 score + softer +1 length when geo alignment strong

package helium314.keyboard.heatmap.swipe

import helium314.keyboard.keyboard.Keyboard
import helium314.keyboard.latin.DictionaryFacilitator
import helium314.keyboard.latin.NgramContext
import helium314.keyboard.latin.SuggestedWords.SuggestedWordInfo
import helium314.keyboard.latin.common.ComposedData
import helium314.keyboard.latin.settings.SettingsValuesForSuggestion
import helium314.keyboard.latin.utils.SuggestionResults

object HeatmapSwipeCandidateSource_v4 {

    private const val SESSION_ID_TYPING = 0
    private const val GEO_WEIGHT = 0.72
    private const val DICT_WEIGHT = 0.28

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
        infer: HeatmapSwipeSegmentInfer_v3.Result,
        facilitator: DictionaryFacilitator,
        ngramContext: NgramContext,
        keyboard: Keyboard,
        settings: SettingsValuesForSuggestion,
        inputStyle: Int,
        maxResults: Int = 32,
    ): List<Pair<SuggestedWordInfo, Double>> {
        val seen = HashSet<String>()
        val scored = ArrayList<Pair<SuggestedWordInfo, Double>>()
        for (prefix in prefixes) {
            if (prefix.isEmpty()) continue
            val results = fetchCompletionsForPrefix(
                prefix, facilitator, ngramContext, keyboard, settings, inputStyle,
            )
            for (info in results) {
                val word = info.mWord?.toString()?.lowercase() ?: continue
                if (!facilitator.isValidSpellingWord(word)) continue
                if (!seen.add(word)) continue
                val geo = HeatmapLiteralSwipeScore_v3.scoreWord(
                    candidate = word,
                    pathLetters = infer.pathLetters,
                    startLabel = infer.startKeyLabel,
                    endLabel = infer.endKeyLabel,
                )
                if (geo <= 0.0) continue
                val dictNorm = normalizeDictScore(info.mScore)
                val lengthMul = lengthMultiplier(word.length, infer.beatCount, geo)
                val combined = (geo * GEO_WEIGHT + dictNorm * DICT_WEIGHT) * lengthMul
                if (combined <= 0.0) continue
                scored.add(info to combined)
            }
            if (scored.size >= maxResults) break
        }
        return scored.sortedByDescending { it.second }.take(maxResults)
    }

    private fun lengthMultiplier(candidateLen: Int, beatCount: Int, geo: Double): Double {
        val base = HeatmapSwipeLengthBand_v1.multiplier(candidateLen, beatCount)
        val delta = kotlin.math.abs(candidateLen - beatCount)
        if (delta == 1 && geo >= 0.82) return base.coerceAtLeast(0.92)
        return base
    }

    private fun normalizeDictScore(raw: Int): Double {
        if (raw <= 0) return 0.0
        return (raw.coerceAtMost(500_000) / 500_000.0).coerceIn(0.0, 1.0)
    }
}
