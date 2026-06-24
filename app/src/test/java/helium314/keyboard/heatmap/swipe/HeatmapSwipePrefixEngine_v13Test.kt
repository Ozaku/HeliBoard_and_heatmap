// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.heatmap.swipe

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class HeatmapSwipePrefixEngine_v13Test {

    @Test
    fun omitsFePrefixWhenThreeKeyPathEndsOnT() {
        val infer = HeatmapSwipeSegmentInfer_v12.Result(
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
                maxBearingChangeDeg = 30.0,
            ),
            maxWordLength = 5,
            normalized = HeatmapPathLettersNormalize_v2.Normalized(
                letters = listOf("f", "e", "t"),
                dwellHints = emptyList(),
            ),
            touchedLetters = setOf("f", "e", "t"),
            touchCounts = mapOf("f" to 10, "e" to 20, "t" to 8),
            rejectedTouchLetters = emptySet(),
            strokeOrderLetters = listOf("f", "e", "t"),
        )
        val variants = HeatmapSwipePrefixEngine_v13.buildPrefixVariants(infer)
        assertFalse(variants.contains("fe"))
        assertFalse(variants.contains("fee"))
        assertTrue(variants.contains("feet"))
    }
}
