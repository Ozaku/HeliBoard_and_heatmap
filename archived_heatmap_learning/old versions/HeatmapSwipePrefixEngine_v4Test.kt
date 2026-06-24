// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.heatmap.swipe

import org.junit.Assert.assertTrue
import org.junit.Test

class HeatmapSwipePrefixEngine_v4Test {

    @Test
    fun insertsDoubleLetterVariantForAccurateOnGeneralStroke() {
        val infer = generalInfer(
            pathLetters = listOf("a", "c", "u", "r", "a", "t", "e"),
            pathLettersRaw = listOf("a", "c", "c", "c", "u", "r", "a", "t", "e"),
        )
        val variants = HeatmapSwipePrefixEngine_v4.buildPrefixVariants(infer)
        assertTrue(variants.contains("accurate"))
    }

    @Test
    fun straightStrokePrefixesCappedAtTwoLetters() {
        val infer = HeatmapSwipeSegmentInfer_v4.Result(
            startKeyLabel = "s",
            pathLetters = listOf("s", "p"),
            pathLettersRaw = listOf("s", "p"),
            endKeyLabel = "p",
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
        val variants = HeatmapSwipePrefixEngine_v4.buildPrefixVariants(infer)
        assertTrue(variants.all { it.length <= 2 })
        assertTrue(variants.contains("sp"))
    }

    private fun generalInfer(
        pathLetters: List<String>,
        pathLettersRaw: List<String>,
    ) = HeatmapSwipeSegmentInfer_v4.Result(
        startKeyLabel = pathLetters.firstOrNull(),
        pathLetters = pathLetters,
        pathLettersRaw = pathLettersRaw,
        endKeyLabel = pathLetters.lastOrNull(),
        beatCount = pathLetters.size,
        beatCountRaw = pathLettersRaw.size,
        classifiedBeats = emptyList(),
        straightLine = HeatmapSwipeStraightLine_v1.Analysis(
            shape = HeatmapSwipeStraightLine_v1.StrokeShape.GENERAL,
            maxWordLength = Int.MAX_VALUE,
            maxBearingChangeDeg = 0.5,
        ),
        maxWordLength = Int.MAX_VALUE,
    )
}
