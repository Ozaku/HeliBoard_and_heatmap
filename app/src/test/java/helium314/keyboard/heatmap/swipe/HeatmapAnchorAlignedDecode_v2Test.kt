// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.heatmap.swipe

import helium314.keyboard.heatmap.learning.HeatmapCoordinateMap_v1
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Verifies the STRICT anchor-aligned lexicon traversal (the heart of the v2 anchor-driven decoder):
 * a candidate word's collapsed-run letters must equal the detected anchor sequence, in order, with
 * doubled letters allowed. These are the exact failures the user reported on beta 0.0.0.54:
 *   eggs(e,g,s) not eras(4 runs); cheese(ee double) not code/come/care; flag not flying/fucking;
 *   pizza(zz double) not pea/piura; eating not ewing/english.
 * Neighbors are empty here so matches are exact, isolating the length/double logic.
 */
class HeatmapAnchorAlignedDecode_v2Test {

    private val lexicon = HeatmapLexiconTrie_v1.fromWordsForTest(
        mapOf(
            // eggs family
            "eggs" to 200, "eras" to 120, "ergs" to 40, "egg" to 150, "ego" to 90,
            // cheese family
            "cheese" to 100, "code" to 180, "come" to 220, "care" to 160, "chee" to 5,
            // flag family
            "flag" to 130, "flying" to 90, "fucking" to 70, "fag" to 20,
            // pizza family
            "pizza" to 110, "pea" to 140, "piura" to 8, "pita" to 30,
            // eating family
            "eating" to 95, "ewing" to 12, "english" to 85, "eat" to 200,
        ),
    )

    private fun strict(vararg anchors: Char): Set<String> =
        lexicon.collectAnchorAlignedWords(
            anchors = anchors.toList(),
            neighbors = emptyMap(),
            maxCandidates = 500,
        ).map { it.word }.toSet()

    @Test
    fun eggsMatchesViaDoubleNotEras() {
        val r = strict('e', 'g', 's')
        assertTrue("eggs (gg double) must match e,g,s", r.contains("eggs"))
        assertFalse("eras has 4 runs and must not match e,g,s", r.contains("eras"))
        assertFalse("ergs has 4 runs and must not match e,g,s", r.contains("ergs"))
        assertFalse("egg lacks the final s anchor", r.contains("egg"))
    }

    @Test
    fun cheeseMatchesViaDoubleNotCodeComeCare() {
        val r = strict('c', 'h', 'e', 's', 'e')
        assertTrue("cheese (ee double over c,h,e,s,e) must match", r.contains("cheese"))
        assertFalse(r.contains("code"))
        assertFalse(r.contains("come"))
        assertFalse(r.contains("care"))
    }

    @Test
    fun flagMatchesNotFlyingOrFucking() {
        val r = strict('f', 'l', 'a', 'g')
        assertTrue("flag must match f,l,a,g", r.contains("flag"))
        assertFalse("flying has 6 runs", r.contains("flying"))
        assertFalse("fucking has 7 runs", r.contains("fucking"))
    }

    @Test
    fun pizzaMatchesViaDoubleNotPeaOrPiura() {
        val r = strict('p', 'i', 'z', 'a')
        assertTrue("pizza (zz double over p,i,z,a) must match", r.contains("pizza"))
        assertFalse("pea is p,e,a — wrong letters/length", r.contains("pea"))
        assertFalse("piura has 5 runs", r.contains("piura"))
    }

    @Test
    fun eatingMatchesNotEwingOrEnglish() {
        val r = strict('e', 'a', 't', 'i', 'n', 'g')
        assertTrue("eating must match e,a,t,i,n,g", r.contains("eating"))
        assertFalse("ewing has 5 runs / wrong letters", r.contains("ewing"))
        assertFalse("english needs a different anchor set", r.contains("english"))
        assertFalse("eat is too short", r.contains("eat"))
    }

    @Test
    fun atMostOneDoublePerAnchor() {
        // word "eee..." style: only single doubling per anchor allowed. e,e anchors would be deduped
        // upstream; here we confirm a triple cannot satisfy a 2-anchor sequence.
        val tri = HeatmapLexiconTrie_v1.fromWordsForTest(mapOf("aa" to 10, "aaa" to 10, "aab" to 10))
        val r = tri.collectAnchorAlignedWords(listOf('a', 'b'), emptyMap(), 50).map { it.word }.toSet()
        assertTrue("aab = a(double a),b matches a,b", r.contains("aab"))
        assertFalse("aaa never reaches anchor b", r.contains("aaa"))
        assertFalse("aa lacks anchor b", r.contains("aa"))
    }

    // --- Anchor extraction on a real 0.0.0.53 "now" trace (start + corner + end) ---
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
        val snapshot = HeatmapCoordinateMap_v1.Snapshot("en", "", "qwerty", 0, 1080, 460, "t53", keys)
        return HeatmapGestureKeyModel_v1.from(snapshot)
    }

    @Test
    fun anchorExtractionOnRealNowTrace() {
        val spec =
            "741,431,0 749,414,74 758,394,79 780,347,91 792,324,96 819,272,107 831,251,112 " +
                "858,205,124 874,179,132 886,157,141 903,129,158 916,109,174 928,90,199 " +
                "890,81,266 847,81,274 799,86,282 747,94,291 715,98,296 693,102,299 638,110,308 " +
                "605,114,312 581,117,316 526,122,324 494,125,329 472,126,333 419,129,341 " +
                "390,130,346 370,130,349 325,131,358 303,131,362 256,131,374 233,130,382 " +
                "214,129,396 212,107,449"
        val pts = ArrayList<DoubleArray>()
        val ts = ArrayList<Int>()
        for (triple in spec.trim().split(" ")) {
            val (x, y, t) = triple.split(",").map { it.toInt() }
            pts.add(doubleArrayOf(x.toDouble(), y.toDouble()))
            ts.add(t)
        }
        val res = HeatmapGestureAnchors_v2.extract(pts, ts.toIntArray(), keyModel53())
        assertEquals("start anchor should be n", 'n', res.keys.first())
        assertEquals("end anchor should be w", 'w', res.keys.last())
        assertTrue("now should produce ~3 anchors, got ${res.keys}", res.keys.size in 2..4)
    }
}
