// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.heatmap.swipe

import helium314.keyboard.latin.common.InputPointers
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class HeatmapSwipeKeyWiggleDetector_v1Test {

    @Test
    fun expandPathDoublesInsertsSecondE() {
        val expanded = HeatmapSwipeKeyWiggleDetector_v1.expandPathDoubles(
            listOf("f", "e", "t"),
            listOf(HeatmapSwipeKeyWiggleDetector_v1.Hint("e", 1, 2)),
        )
        assertEquals(listOf("f", "e", "e", "t"), expanded)
    }

    @Test
    fun zigzagSamplesOnMiddleKeyProduceHint() {
        val coords = listOf(
            20 to 20,
            50 to 20,
            100 to 0,
            110 to 30,
            120 to 0,
            130 to 30,
            140 to 20,
            200 to 20,
        )
        val p = InputPointers(coords.size)
        coords.forEachIndexed { i, (x, y) ->
            p.addPointer(x, y, i, 0)
        }
        val layout = fetLayout()
        val hints = HeatmapSwipeKeyWiggleDetector_v1.detect(
            layout,
            p,
            listOf("f", "e", "t"),
        )
        assertTrue(hints.any { it.letter == "e" })
    }

    private fun fetLayout(): helium314.keyboard.heatmap.learning.HeatmapCoordinateMap_v1.Snapshot =
        helium314.keyboard.heatmap.learning.HeatmapCoordinateMap_v1.Snapshot(
            localeTag = "en",
            layoutSetExtra = "",
            mainLayoutName = "qwerty",
            elementId = 1,
            keyboardWidth = 1080,
            keyboardHeight = 600,
            layoutHash = "test-fet",
            keys = listOf(
                helium314.keyboard.heatmap.learning.HeatmapCoordinateMap_v1.KeyBoundsEntry(
                    "f", 102, 0, 0, 60, 60,
                ),
                helium314.keyboard.heatmap.learning.HeatmapCoordinateMap_v1.KeyBoundsEntry(
                    "e", 101, 70, -10, 170, 50,
                ),
                helium314.keyboard.heatmap.learning.HeatmapCoordinateMap_v1.KeyBoundsEntry(
                    "t", 116, 190, 0, 260, 60,
                ),
            ),
        )
}
