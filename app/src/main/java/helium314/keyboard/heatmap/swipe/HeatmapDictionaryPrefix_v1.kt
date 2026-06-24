// SPDX-License-Identifier: GPL-3.0-only

// ai-note: Block 3 step 14 v1 — filter legacy gesture suggestions by inferred prefix + score

package helium314.keyboard.heatmap.swipe

import helium314.keyboard.latin.DictionaryFacilitator
import helium314.keyboard.latin.SuggestedWords.SuggestedWordInfo

object HeatmapDictionaryPrefix_v1 {

    fun filterAndRank(
        facilitator: DictionaryFacilitator,
        suggestions: List<SuggestedWordInfo>,
        infer: HeatmapSwipeSegmentInfer_v1.Result,
        maxResults: Int = 8,
    ): List<SuggestedWordInfo> {
        if (suggestions.isEmpty()) return suggestions
        val start = infer.startKeyLabel
        val prefix = buildPrefixFromPath(infer.pathLetters, infer.beatCount)
        val scored = suggestions.mapNotNull { info ->
            val word = info.mWord?.toString() ?: return@mapNotNull null
            val lower = word.lowercase()
            if (!start.isNullOrEmpty() && !lower.startsWith(start)) return@mapNotNull null
            if (prefix.length >= 2 && !lower.startsWith(prefix.take(2))) {
                // soft prefix: at least first two inferred letters in order
                if (!lettersInOrder(lower, infer.pathLetters)) return@mapNotNull null
            }
            if (!facilitator.isValidSpellingWord(lower)) return@mapNotNull null
            val geo = HeatmapLiteralSwipeScore_v1.scoreWord(
                candidate = lower,
                pathLetters = infer.pathLetters,
                startLabel = infer.startKeyLabel,
                endLabel = infer.endKeyLabel,
            )
            if (geo <= 0.0) return@mapNotNull null
            info to geo
        }.sortedByDescending { it.second }
        return scored.take(maxResults).map { it.first }
    }

    private fun buildPrefixFromPath(pathLetters: List<String>, beatCount: Int): String {
        if (pathLetters.isEmpty()) return ""
        val take = beatCount.coerceAtMost(pathLetters.size).coerceAtLeast(1)
        return pathLetters.take(take).joinToString("")
    }

    private fun lettersInOrder(word: String, letters: List<String>): Boolean {
        var idx = 0
        for (label in letters) {
            val pos = word.indexOf(label, idx)
            if (pos < 0) return false
            idx = pos + 1
        }
        return true
    }
}
