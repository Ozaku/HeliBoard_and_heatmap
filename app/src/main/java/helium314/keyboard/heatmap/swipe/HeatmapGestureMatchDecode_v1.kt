// SPDX-License-Identifier: GPL-3.0-only

// ai-note: v1 — SHARK2-style gesture-template decoder. Replaces the corner-extraction
// pipeline (HeatmapLiteralSwipeDecode_v25). Instead of guessing discrete letters then doing
// a prefix lookup, it scores whole dictionary words against the whole finger path:
//   1. enumerate the tap dictionary into HeatmapLexiconTrie_v1 (cached)
//   2. prune to words that are a neighbor-tolerant in-order subsequence of the visited keys
//   3. rank survivors by location + shape + arc-length + start/end anchors, blended with freq
// Falls back to prefix completion queries only while the lexicon is still building.
// Returns the same DecodeResult shape consumed by Suggest.kt; persists the decoded word as the
// intent path (fixing the old tuningRevision:1 / garbage-path mismatch in word memory).
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
import helium314.keyboard.latin.dictionary.Dictionary
import helium314.keyboard.latin.settings.SettingsValuesForSuggestion
import helium314.keyboard.latin.utils.Log
import kotlin.math.pow

object HeatmapGestureMatchDecode_v1 {

    private const val TAG = "HeatmapGestureMatch"
    private const val SESSION_ID_TYPING = 0
    private const val SCORE_SCALE = 900_000.0
    private const val MAX_DICT_PROBABILITY = 255.0

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
        return try {
            decodeInternal(keyboard, pointers, facilitator, ngramContext, settings, inputStyle)
        } catch (e: Exception) {
            Log.e(TAG, "decode crashed — returning empty", e)
            DecodeResult(emptyList(), null, null)
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
        val infer = HeatmapSwipeSegmentInfer_v21.infer(keyboard, pointers)
            ?: return DecodeResult(emptyList(), null, null)
        val layout = HeatmapCoordinateMap_v1.fromKeyboard(keyboard)
            ?: return DecodeResult(emptyList(), infer, null)
        val pointCount = pointers.pointerSize
        if (pointCount < 2) return DecodeResult(emptyList(), infer, null)

        val keyModel = HeatmapGestureKeyModel_v1.from(layout)
        val points = readPoints(pointers, pointCount)
        val times = readTimes(pointers, pointCount)
        val features = HeatmapGestureShapeScore_v1.features(points, keyModel, times)

        val cacheKey = pointCount.toString() + "|" +
            points.first().joinToString(",") + "|" + points.last().joinToString(",")
        if (isPreview && previewCacheKey == cacheKey && previewCache != null) {
            return previewCache!!
        }
        if (isTail) {
            previewCacheKey = null
            previewCache = null
        }

        val resultCap = if (isPreview) {
            HeatmapGestureTuningConstants_v1.MAX_RESULTS_PREVIEW
        } else {
            HeatmapGestureTuningConstants_v1.MAX_RESULTS_FULL
        }

        val trie = HeatmapLexiconTrie_v1.getOrTriggerBuild(facilitator)
        val ranked: List<SuggestedWordInfo>
        val visitedStr: String
        if (trie != null) {
            val gen = HeatmapTrieCandidateGen_v1.generate(points, keyModel, trie, infer.startKeyLabel?.firstOrNull())
            visitedStr = gen.visited.joinToString("")
            ranked = rankTrieCandidates(gen.candidates, features, facilitator, resultCap)
            Log.i(
                TAG,
                "trie lexicon=${trie.size} visited=$visitedStr cands=${gen.candidates.size} " +
                    "top=${ranked.firstOrNull()?.mWord} tuning=${HeatmapGestureTuningConstants_v1.TUNING_REVISION}",
            )
        } else {
            val visited = HeatmapVisitedKeySequence_v1.build(points, keyModel)
            visitedStr = visited.joinToString("")
            ranked = rankFallback(
                visited, features, facilitator, ngramContext, keyboard, settings, inputStyle, resultCap,
            )
            Log.i(TAG, "fallback (lexicon building) visited=$visitedStr top=${ranked.firstOrNull()?.mWord}")
        }

        val topWord = ranked.firstOrNull()?.mWord?.toString().orEmpty()
        val decodedLetters = topWord.filter { it.isLetter() }.map { it.toString() }
        val stashInfer = if (decodedLetters.isNotEmpty()) {
            infer.copy(intentPathLetters = decodedLetters, pathLetters = decodedLetters)
        } else {
            infer
        }
        val diagnostics = buildDiagnostics(stashInfer, visitedStr, ranked, pointCount)
        val runnerUps = ranked.map { it.mWord?.toString().orEmpty() }
        HeatmapSwipeDecodeSnapshot_v3.stash(stashInfer, runnerUps, diagnostics)

        val result = DecodeResult(ranked, stashInfer, diagnostics)
        if (isPreview) {
            previewCacheKey = cacheKey
            previewCache = result
        }
        return result
    }

    private fun readPoints(pointers: InputPointers, count: Int): List<DoubleArray> {
        val xs = pointers.xCoordinates
        val ys = pointers.yCoordinates
        val out = ArrayList<DoubleArray>(count)
        for (i in 0 until count) {
            out.add(doubleArrayOf(xs[i].toDouble(), ys[i].toDouble()))
        }
        return out
    }

    private fun readTimes(pointers: InputPointers, count: Int): IntArray {
        val t = pointers.times
        return if (t.size >= count) t.copyOf(count) else IntArray(count) { it * 16 }
    }

    private fun rankTrieCandidates(
        candidates: List<HeatmapLexiconTrie_v1.Candidate>,
        features: HeatmapGestureShapeScore_v1.GestureFeatures,
        facilitator: DictionaryFacilitator,
        resultCap: Int,
    ): List<SuggestedWordInfo> {
        val sourceDict: Dictionary = facilitator.mainDictionary ?: return emptyList()
        val scored = ArrayList<Pair<String, Double>>(candidates.size)
        for (cand in candidates) {
            val geo = HeatmapGestureShapeScore_v1.score(cand.word, features)
            if (geo <= 0.0) continue
            val combined = geo * freqWeight(cand.frequency)
            if (combined <= 0.0) continue
            scored.add(cand.word to combined)
        }
        scored.sortByDescending { it.second }
        val out = ArrayList<SuggestedWordInfo>(resultCap)
        for (i in 0 until minOf(resultCap, scored.size)) {
            out.add(makeInfo(scored[i].first, scored[i].second, sourceDict))
        }
        return out
    }

    private fun rankFallback(
        visited: List<Char>,
        features: HeatmapGestureShapeScore_v1.GestureFeatures,
        facilitator: DictionaryFacilitator,
        ngramContext: NgramContext,
        keyboard: Keyboard,
        settings: SettingsValuesForSuggestion,
        inputStyle: Int,
        resultCap: Int,
    ): List<SuggestedWordInfo> {
        if (visited.isEmpty()) return emptyList()
        val prefixes = LinkedHashSet<String>()
        val sb = StringBuilder()
        for (i in 0 until minOf(4, visited.size)) {
            sb.append(visited[i])
            if (sb.length >= 1) prefixes.add(sb.toString())
        }
        prefixes.add(visited.first().toString())
        val seen = HashSet<String>()
        val scored = ArrayList<Pair<SuggestedWordInfo, Double>>()
        for (prefix in prefixes) {
            val composed = ComposedData.createForWord(prefix)
            val results = facilitator.getSuggestionResults(
                composed, ngramContext, keyboard, settings, SESSION_ID_TYPING, inputStyle,
            )
            for (info in results) {
                val word = info.mWord?.toString() ?: continue
                if (info.mSourceDict == null) continue
                if (!seen.add(word.lowercase())) continue
                val geo = HeatmapGestureShapeScore_v1.score(word, features)
                if (geo <= 0.0) continue
                val combined = geo * freqWeight(normalizeDictScore(info.mScore))
                if (combined <= 0.0) continue
                scored.add(info to combined)
            }
        }
        scored.sortByDescending { it.second }
        return scored.take(resultCap).map { (info, combined) ->
            makeInfo(info.mWord.toString(), combined, info.mSourceDict)
        }
    }

    private fun freqWeight(rawFrequency: Int): Double {
        val norm = (rawFrequency.coerceAtLeast(1) / MAX_DICT_PROBABILITY).coerceIn(0.0, 1.0)
        return norm.pow(HeatmapGestureTuningConstants_v1.FREQ_ALPHA)
    }

    private fun normalizeDictScore(raw: Int): Int {
        if (raw <= 0) return 1
        // Map the native suggestion score (large ints) down to a 0..255-ish frequency proxy.
        return (raw.coerceAtMost(500_000) * 255 / 500_000).coerceAtLeast(1)
    }

    private fun makeInfo(word: String, combined: Double, sourceDict: Dictionary): SuggestedWordInfo {
        val score = (combined * SCORE_SCALE).toInt().coerceAtLeast(1)
        return SuggestedWordInfo(
            word,
            "",
            score,
            SuggestedWordInfo.KIND_CORRECTION,
            sourceDict,
            SuggestedWordInfo.NOT_AN_INDEX,
            SuggestedWordInfo.NOT_A_CONFIDENCE,
        )
    }

    private fun buildDiagnostics(
        infer: HeatmapSwipeSegmentInfer_v19.Result,
        visitedStr: String,
        ranked: List<SuggestedWordInfo>,
        pointCount: Int,
    ): HeatmapSwipeDecodeDiagnostics_v1.Bundle {
        val stashPointCount = HeatmapPathCapture_v3.peekPointerSize()
        return HeatmapSwipeDecodeDiagnostics_v1.Bundle(
            tuningRevision = HeatmapGestureTuningConstants_v1.TUNING_REVISION,
            pathLen = infer.intentPathLetters.size,
            strokeLen = visitedStr.length,
            intentPath = infer.intentPathLetters.joinToString(""),
            visitOrder = visitedStr,
            transitKeyCount = 0,
            dwellSegments = infer.kinematics.dwellSegments,
            prefixesTried = emptyList(),
            topCandidates = ranked.take(5).map {
                HeatmapSwipeDecodeDiagnostics_v1.CandidateDiag(
                    it.mWord?.toString().orEmpty(),
                    it.mScore / SCORE_SCALE,
                )
            },
            avgSpeedKeyWidthsPerSec = infer.kinematics.avgSpeedKeyWidthsPerSec,
            isSlowStroke = infer.kinematics.isSlowStroke,
            decodePointCount = pointCount,
            stashPointCount = stashPointCount,
            pointerIntegrityOk = stashPointCount <= 0 || pointCount == stashPointCount,
        )
    }
}
