// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.heatmap.swipe

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class HeatmapSwipeEndLetterPolicy_v2Test {

    private fun infer(
        path: List<String>,
        beatCountRaw: Int = path.size,
        end: String = path.last(),
        touched: Set<String> = path.toSet(),
    ): HeatmapSwipeSegmentInfer_v12.Result =
        HeatmapSwipeSegmentInfer_v12.Result(
            startKeyLabel = path.firstOrNull(),
            pathLetters = path,
            pathLettersRaw = path,
            endKeyLabel = end,
            beatCount = path.size,
            beatCountRaw = beatCountRaw,
            classifiedBeats = emptyList(),
            straightLine = HeatmapSwipeStraightLine_v1.Analysis(
                shape = HeatmapSwipeStraightLine_v1.StrokeShape.GENERAL,
                maxWordLength = path.size + 2,
                maxBearingChangeDeg = 45.0,
            ),
            maxWordLength = path.size + 2,
            normalized = HeatmapPathLettersNormalize_v2.Normalized(path, emptyList()),
            touchedLetters = touched,
            touchCounts = emptyMap(),
            rejectedTouchLetters = emptySet(),
            strokeOrderLetters = path,
        )

    @Test
    fun requiresEndForShortWigglePath() {
        assertTrue(HeatmapSwipeEndLetterPolicy_v2.requiresEndMatch(infer(listOf("f", "e", "e", "t"), beatCountRaw = 6)))
    }

    @Test
    fun doesNotRequireEndForLongWords() {
        assertFalse(
            HeatmapSwipeEndLetterPolicy_v2.requiresEndMatch(
                infer(listOf("f", "e", "e", "d", "i", "n", "g")),
            ),
        )
        assertFalse(
            HeatmapSwipeEndLetterPolicy_v2.requiresEndMatch(
                infer(listOf("t", "y", "p", "i", "n", "g")),
            ),
        )
    }
}
