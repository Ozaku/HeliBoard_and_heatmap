// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.heatmap.swipe

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class HeatmapSwipePrefixEngine_v9Test {

    @Test
    fun straightTwoKeyOmitsStartOnlyPrefix() {
        val infer = HeatmapSwipeSegmentInfer_v8.Result(
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
            normalized = HeatmapPathLettersNormalize_v2.Normalized(
                letters = listOf("i", "m"),
                dwellHints = emptyList(),
            ),
        )
        val variants = HeatmapSwipePrefixEngine_v9.buildPrefixVariants(infer)
        assertTrue(variants.contains("im"))
        assertFalse(variants.any { it.length == 1 })
    }

    @Test
    fun fetPathIncludesFeetVariant() {
        val infer = HeatmapSwipeSegmentInfer_v8.Result(
            startKeyLabel = "f",
            pathLetters = listOf("f", "e", "t"),
            pathLettersRaw = listOf("f", "e", "t"),
            endKeyLabel = "t",
            beatCount = 3,
            beatCountRaw = 3,
            classifiedBeats = emptyList(),
            straightLine = HeatmapSwipeStraightLine_v1.Analysis(
                shape = HeatmapSwipeStraightLine_v1.StrokeShape.GENERAL,
                maxWordLength = 5,
                maxBearingChangeDeg = 40.0,
            ),
            maxWordLength = 5,
            normalized = HeatmapPathLettersNormalize_v2.Normalized(
                letters = listOf("f", "e", "t"),
                dwellHints = emptyList(),
            ),
        )
        val variants = HeatmapSwipePrefixEngine_v9.buildPrefixVariants(infer)
        assertTrue(variants.contains("feet"))
    }
}
