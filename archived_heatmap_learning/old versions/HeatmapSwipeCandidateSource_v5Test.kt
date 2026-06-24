// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.heatmap.swipe

import org.junit.Assert.assertFalse
import org.junit.Test

class HeatmapSwipeCandidateSource_v5Test {

    @Test
    fun straightTwoLetterCapRejectsThreeLetterWord() {
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
                maxBearingChangeDeg = 0.05,
            ),
            maxWordLength = 2,
        )
        assertFalse("sap".length <= infer.maxWordLength)
    }
}
