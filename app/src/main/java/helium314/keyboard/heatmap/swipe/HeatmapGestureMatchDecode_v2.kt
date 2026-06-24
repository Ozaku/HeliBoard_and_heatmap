// SPDX-License-Identifier: GPL-3.0-only

// ai-note: v2 — ANCHOR-DRIVEN gesture decoder. v1 generated candidates from the full transit
// crossing sequence and used anchors only as a weak score multiplier, so words were built from
// keys the finger merely passed over (eggs->eras, pizza->piura, flag->flying). v2 makes the
// detected anchors (start + velocity-minima hard stops + sharp corners + end) the PRIMARY driver:
//   1. HeatmapGestureAnchors_v2 -> ordered anchor keys (the intended letters)
//   2. HeatmapAnchorCandidateGen_v1 -> STRICT pool: collapsed-run letters == anchor count
//      (doubles allowed); graded fallback only when strict is empty
//   3. rank survivors by location+shape (HeatmapGestureShapeScore_v1) + light frequency tiebreak;
//      fallback-tier candidates are geometrically penalized so strict always wins
// Same DecodeResult shape as v1; persists the decoded word as the intent path. Tuning rev 7.
// AI EDIT MAP:
//   decode()/decodeInternal() -> entry; readPoints/readTimes -> InputPointers -> path
//   rankAnchorCandidates()    -> strict/fallback ranking with tier penalty
//   rankFallback()            -> lexicon-still-building prefix path (mirrors v1)
package helium314.keyboard.heatmap.swipe

import helium314.keyboard.heatmap.learning.HeatmapUserProfile_v1
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

object HeatmapGestureMatchDecode_v2 {

    private const val TAG = "HeatmapGestureMatch2"
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
            HeatmapGestureTuningConstants_v2.MAX_RESULTS_PREVIEW
        } else {
            HeatmapGestureTuningConstants_v2.MAX_RESULTS_FULL
        }

        val trie = HeatmapLexiconTrie_v1.getOrTriggerBuild(facilitator)
        val ranked: List<SuggestedWordInfo>
        val anchorStr: String
        var strictCount = 0
        var tier = HeatmapAnchorCandidateGen_v1.Tier.NONE
        if (trie != null) {
            val gen = HeatmapAnchorCandidateGen_v1.generate(
                points, times, keyModel, trie, infer.startKeyLabel?.firstOrNull(),
            )
            anchorStr = gen.anchorKeys.joinToString("")
            strictCount = gen.strict.size
            tier = gen.tier

            val strictScored = scoreAnchorCandidates(gen.strict, features, 1.0, anchorStr)
            val fallbackScored = scoreAnchorCandidates(gen.fallback, features, HeatmapGestureTuningConstants_v2.FALLBACK_GEO_PENALTY, anchorStr)

            val merged = ArrayList<Pair<String, Double>>(strictScored.size + fallbackScored.size)
            merged.addAll(strictScored)
            val strictWords = strictScored.map { it.first }.toSet()
            for (fb in fallbackScored) {
                if (fb.first !in strictWords) merged.add(fb)
            }
            merged.sortByDescending { it.second }

            val sourceDict: Dictionary? = facilitator.mainDictionary
            val out = ArrayList<SuggestedWordInfo>(resultCap)
            if (sourceDict != null) {
                for (i in 0 until minOf(resultCap, merged.size)) {
                    out.add(makeInfo(merged[i].first, merged[i].second, sourceDict))
                }
            }
            ranked = out
            Log.i(
                TAG,
                "anchors=$anchorStr tier=$tier strict=$strictCount cands=${gen.strict.size + gen.fallback.size} " +
                    "top=${ranked.firstOrNull()?.mWord} tuning=${HeatmapGestureTuningConstants_v2.TUNING_REVISION}",
            )
        } else {
            val visited = HeatmapVisitedKeySequence_v1.build(points, keyModel)
            anchorStr = visited.joinToString("")
            ranked = rankFallback(
                visited, features, facilitator, ngramContext, keyboard, settings, inputStyle, resultCap,
            )
            Log.i(TAG, "fallback (lexicon building) visited=$anchorStr top=${ranked.firstOrNull()?.mWord}")
        }

        val topWord = ranked.firstOrNull()?.mWord?.toString().orEmpty()
        val decodedLetters = topWord.filter { it.isLetter() }.map { it.toString() }
        val stashInfer = if (decodedLetters.isNotEmpty()) {
            infer.copy(intentPathLetters = decodedLetters, pathLetters = decodedLetters)
        } else {
            infer
        }
        val diagnostics = buildDiagnostics(stashInfer, anchorStr, ranked, pointCount, strictCount, tier)
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

    private fun scoreAnchorCandidates(
        candidates: List<HeatmapLexiconTrie_v1.Candidate>,
        features: HeatmapGestureShapeScore_v1.GestureFeatures,
        tierPenalty: Double,
        anchorStr: String,
    ): List<Pair<String, Double>> {
        val scored = ArrayList<Pair<String, Double>>(candidates.size)
        for (cand in candidates) {
            val geo = HeatmapGestureShapeScore_v1.score(cand.word, features)
            if (geo <= 0.0) continue
            
            // Apply Heatmap User Profile boosts
            val shapeBoost = HeatmapUserProfile_v1.getShapeBoost(anchorStr, cand.word)
            val globalBoost = HeatmapUserProfile_v1.getGlobalBoost(cand.word)
            
            // A shape boost is extremely strong evidence of intent.
            // A global boost is a mild frequency bump.
            val personalMultiplier = 1.0 + (shapeBoost * 5.0) + (globalBoost * 0.1)
            
            val combined = geo * tierPenalty * freqWeight(cand.frequency) * personalMultiplier
            if (combined <= 0.0) continue
            scored.add(cand.word to combined)
        }
        return scored
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
        return norm.pow(HeatmapGestureTuningConstants_v2.FREQ_ALPHA)
    }

    private fun normalizeDictScore(raw: Int): Int {
        if (raw <= 0) return 1
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
        anchorStr: String,
        ranked: List<SuggestedWordInfo>,
        pointCount: Int,
        strictCount: Int,
        tier: HeatmapAnchorCandidateGen_v1.Tier,
    ): HeatmapSwipeDecodeDiagnostics_v1.Bundle {
        val stashPointCount = HeatmapPathCapture_v3.peekPointerSize()
        return HeatmapSwipeDecodeDiagnostics_v1.Bundle(
            tuningRevision = HeatmapGestureTuningConstants_v2.TUNING_REVISION,
            pathLen = infer.intentPathLetters.size,
            strokeLen = anchorStr.length,
            intentPath = infer.intentPathLetters.joinToString(""),
            // ai-note: visitOrder now carries the ANCHOR key sequence (intended letters), not the
            // noisy transit crossing string the v1 decoder logged here.
            visitOrder = anchorStr,
            transitKeyCount = strictCount,
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
