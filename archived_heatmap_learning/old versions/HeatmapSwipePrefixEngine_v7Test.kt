// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.heatmap.swipe

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class HeatmapSwipePrefixEngine_v7Test {

    @Test
    fun straightTwoKeyOmitsStartOnlyPrefix() {
        val infer = HeatmapSwipeSegmentInfer_v6.Result(
            startKeyLabel = "i",
            pathLetters = listOf("i", "m"),
            pathLettersRaw = listOf("i", "m"),
            endKeyLabel = "m",
            beatCount = 2,
            beatCountRaw = 2,
            classifiedBeats = emptyList(),
            straightLine = HeatmapSwipeStraightLine_v1.Analysis(
                shape = HeatmapSwipeStraightLine_v1.StrokeShape.NEAR_STRAIGHT_TWO_LETTER,
                maxWordLength = 2,
                maxBearingChangeDeg = 5.0,
            ),
            maxWordLength = 2,
        )
        val variants = HeatmapSwipePrefixEngine_v7.buildPrefixVariants(infer)
        assertTrue(variants.contains("im"))
        assertFalse(variants.any { it.length == 1 })
    }
}
