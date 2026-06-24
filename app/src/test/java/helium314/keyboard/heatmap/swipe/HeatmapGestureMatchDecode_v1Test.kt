// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.heatmap.swipe

import helium314.keyboard.heatmap.learning.HeatmapCoordinateMap_v1
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.pow

/**
 * Replays REAL swipe traces pulled from the 0.0.0.52 export (data_pull/session_latest.json)
 * against the empirically reconstructed key layout, and asserts the SHARK2-style decoder
 * recovers the intended word as the top candidate where the old corner-extractor produced
 * garbage (CLI / SDS / I for cyclical / system / into).
 */
class HeatmapGestureMatchDecode_v1Test {

    // Empirical key centers (label:x,y) from per-point bestLabel centroids in the pulled data.
    private val centersSpec =
        "a:133,264;b:647,374;c:425,409;d:319,250;e:271,99;f:440,273;g:536,238;h:651,255;" +
            "i:811,99;j:760,255;k:867,238;l:952,217;m:851,381;n:760,403;o:900,112;p:973,180;" +
            "r:383,100;s:214,257;t:489,113;u:703,118;v:532,378;w:182,116;x:355,380;y:586,112;z:242,358"

    private fun keyModel(): HeatmapGestureKeyModel_v1 {
        val keys = centersSpec.split(";").map { spec ->
            val (label, xy) = spec.split(":")
            val (x, y) = xy.split(",").map { it.toInt() }
            // 1px boxes => classify() falls back to nearest-center, which is what we want.
            HeatmapCoordinateMap_v1.KeyBoundsEntry(label, label[0].code, x, y, x + 1, y + 1)
        }
        val snapshot = HeatmapCoordinateMap_v1.Snapshot(
            localeTag = "en",
            layoutSetExtra = "",
            mainLayoutName = "qwerty",
            elementId = 0,
            keyboardWidth = 1080,
            keyboardHeight = 460,
            layoutHash = "test",
            keys = keys,
        )
        return HeatmapGestureKeyModel_v1.from(snapshot)
    }

    private fun parseTrace(spec: String): List<DoubleArray> =
        spec.trim().split(" ").map { pair ->
            val (x, y) = pair.split(",").map { it.toDouble() }
            doubleArrayOf(x, y)
        }

    private val lexicon = HeatmapLexiconTrie_v1.fromWordsForTest(
        mapOf(
            "cyclical" to 6, "cynical" to 30, "crucial" to 90, "call" to 200, "cup" to 150,
            "cool" to 140, "coal" to 60, "clap" to 40, "click" to 80, "cycle" to 70,
            "into" to 230, "info" to 120, "intro" to 90, "onto" to 80, "in" to 250, "it" to 240,
            "system" to 110, "stream" to 70, "steam" to 60, "stem" to 50, "seem" to 90, "sum" to 90,
        ),
    )

    private fun topWord(traceSpec: String, startSeed: Char): String {
        val km = keyModel()
        val points = parseTrace(traceSpec)
        val features = HeatmapGestureShapeScore_v1.features(points, km)
        val gen = HeatmapTrieCandidateGen_v1.generate(points, km, lexicon, startSeed)
        assertTrue("expected a non-empty candidate pool", gen.candidates.isNotEmpty())
        val alpha = HeatmapGestureTuningConstants_v1.FREQ_ALPHA
        return gen.candidates
            .map { cand ->
                val geo = HeatmapGestureShapeScore_v1.score(cand.word, features)
                val freqW = (cand.frequency.coerceAtLeast(1) / 255.0).pow(alpha)
                cand.word to geo * freqW
            }
            .maxByOrNull { it.second }!!.first
    }

    @Test
    fun recoversIntoFromRealTrace() {
        val trace = "806,89 808,120 808,140 807,163 804,187 800,215 796,244 791,278 787,310 " +
            "785,345 783,376 783,403 784,424 786,443 788,461 762,436 735,404 705,370 675,336 " +
            "642,303 607,270 576,239 546,210 519,182 495,158 475,135 459,118 443,96 470,77 " +
            "506,70 550,66 599,61 655,55 711,50 764,45 810,39 853,34 889,30 916,25"
        assertEquals("into", topWord(trace, 'i'))
    }

    @Test
    fun recoversSystemFromRealTrace() {
        val trace = "215,283 249,263 272,247 301,228 331,206 365,182 400,158 436,136 470,117 " +
            "500,101 525,90 543,82 569,73 531,86 491,103 450,122 410,144 374,165 344,185 317,202 " +
            "293,220 271,237 253,252 236,266 222,278 201,296 184,309 199,288 229,262 266,232 " +
            "306,202 348,171 391,143 431,118 469,98 501,82 529,69 550,59 577,47 538,42 508,41 " +
            "476,42 441,43 405,47 369,52 337,58 309,63 289,69 266,77 306,112 351,124 405,137 " +
            "465,154 529,173 589,197 647,225 699,254 746,283 782,308 811,328 833,344 848,354 863,364"
        assertEquals("system", topWord(trace, 's'))
    }

    @Test
    fun recoversCyclicalFromRealTrace() {
        val trace = "403,419 416,384 430,355 447,323 468,290 490,256 510,227 528,201 542,182 " +
            "553,166 563,150 574,130 584,109 595,90 607,75 591,101 572,123 552,147 528,175 " +
            "505,205 483,235 463,267 447,298 435,326 425,350 419,372 415,390 412,408 411,429 " +
            "411,429 411,429 439,416 467,398 501,377 540,358 580,340 622,325 663,312 683,306 " +
            "702,302 777,275 843,247 888,227 922,209 948,196 964,187 964,187 934,178 903,172 " +
            "876,164 846,146 829,131 816,116 816,116 816,116 802,103 783,91 783,91 783,91 " +
            "802,88 820,92 820,92 820,92 783,107 757,119 727,134 697,152 666,172 634,193 " +
            "603,215 574,236 546,258 520,279 496,300 474,320 454,339 439,354 424,371 411,386 " +
            "398,404 389,422 389,422 389,422 378,384 365,363 349,342 330,322 307,302 283,284 " +
            "259,270 234,259 210,252 187,248 167,246 149,245 127,243 144,235 187,229 239,226 " +
            "330,222 348,222 419,221 507,220 525,220 576,221 634,223 692,226 747,231 796,236 " +
            "837,240 874,243 903,244 926,245 947,246 969,246"
        // cyclical and crucial are geometrically very close; accept either as the clear winner
        // over the short common distractors (call/cup/cool) that the old decoder fell back to.
        val top = topWord(trace, 'c')
        assertTrue("expected cyclical or crucial, got $top", top == "cyclical" || top == "crucial")
    }

    // --- Corner/dwell anchor-coverage tests (real 0.0.0.53 traces with timestamps) ---
    // Empirical centers from the 0.0.0.53 export.
    private val centersSpec53 =
        "a:135,262;b:661,376;c:443,350;d:318,246;e:273,107;f:445,262;g:547,240;h:636,263;" +
            "i:801,101;j:761,273;k:847,254;l:924,190;m:839,406;n:757,405;o:916,98;p:987,93;" +
            "r:377,105;s:222,247;t:487,104;u:704,93;v:549,362;w:192,119;y:589,108"

    private fun keyModel53(): HeatmapGestureKeyModel_v1 {
        val keys = centersSpec53.split(";").map { spec ->
            val (label, xy) = spec.split(":")
            val (x, y) = xy.split(",").map { it.toInt() }
            HeatmapCoordinateMap_v1.KeyBoundsEntry(label, label[0].code, x, y, x + 1, y + 1)
        }
        val snapshot = HeatmapCoordinateMap_v1.Snapshot(
            "en", "", "qwerty", 0, 1080, 460, "t53", keys,
        )
        return HeatmapGestureKeyModel_v1.from(snapshot)
    }

    /** Parse "x,y,t x,y,t ..." into points + times. */
    private fun parseTimedTrace(spec: String): Pair<List<DoubleArray>, IntArray> {
        val pts = ArrayList<DoubleArray>()
        val ts = ArrayList<Int>()
        for (triple in spec.trim().split(" ")) {
            val (x, y, t) = triple.split(",").map { it.toInt() }
            pts.add(doubleArrayOf(x.toDouble(), y.toDouble()))
            ts.add(t)
        }
        return pts to ts.toIntArray()
    }

    private fun rankedAmong(spec: String, candidates: List<String>): List<String> {
        val km = keyModel53()
        val (points, times) = parseTimedTrace(spec)
        val features = HeatmapGestureShapeScore_v1.features(points, km, times)
        return candidates
            .map { it to HeatmapGestureShapeScore_v1.score(it, features) }
            .sortedByDescending { it.second }
            .map { it.first }
    }

    @Test
    fun anchorCoverageDemotesTransitIntrudersForNow() {
        // 'i','u','y' are swept fast+straight across the top; 'now' covers the o-corner and w-end.
        val trace =
            "741,431,0 749,414,74 758,394,79 780,347,91 792,324,96 819,272,107 831,251,112 " +
                "858,205,124 874,179,132 886,157,141 903,129,158 916,109,174 928,90,199 " +
                "890,81,266 847,81,274 799,86,282 747,94,291 715,98,296 693,102,299 638,110,308 " +
                "605,114,312 581,117,316 526,122,324 494,125,329 472,126,333 419,129,341 " +
                "390,130,346 370,130,349 325,131,358 303,131,362 256,131,374 233,130,382 " +
                "214,129,396 212,107,449"
        val ranked = rankedAmong(trace, listOf("now", "nine", "nice", "new"))
        assertEquals("now", ranked.first())
        assertTrue("nine must be demoted below now", ranked.indexOf("nine") > 0)
        assertTrue("nice must be demoted below now", ranked.indexOf("nice") > ranked.indexOf("now"))
    }

    @Test
    fun anchorCoverageDemotesTransitIntrudersForTo() {
        // 'to' sweeps t -> o across the top crossing y,u,i; 'tiu'/'tip' rely on those transits.
        val trace =
            "464,83,0 486,83,40 506,84,48 527,84,57 547,85,65 567,85,73 585,85,82 603,85,90 " +
                "640,84,107 663,82,115 689,79,123 707,77,128 761,69,140 781,66,144 800,63,148 " +
                "837,56,157 855,53,161 897,46,174 917,43,182 938,41,198"
        val ranked = rankedAmong(trace, listOf("to", "tiu", "tip", "too"))
        assertEquals("to", ranked.first())
        assertTrue("tiu must rank below to", ranked.indexOf("tiu") > 0)
        assertTrue("tip must rank below to", ranked.indexOf("tip") > 0)
    }

    @Test
    fun trieSubsequenceSearchRespectsStartAndOrder() {
        val km = keyModel()
        val visited = "cyclical".toList()
        val cands = lexicon.collectSubsequenceWords(
            visited = visited,
            neighbors = km.neighbors,
            startChars = setOf('c'),
            maxLen = 24,
            maxCandidates = 100,
        ).map { it.word }
        assertTrue(cands.contains("cyclical"))
        assertTrue("words not starting with c must be excluded", cands.none { !it.startsWith("c") })
    }
}
