// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.heatmap.swipe

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class HeatmapPathLettersNormalize_v9Test {

    @Test
    fun multiCornerPrefersLongestVisitOrder() {
        val visit = listOf("f", "l", "y", "i", "n", "g")
        val beat = listOf("f", "y", "n", "g")
        assertTrue(HeatmapPathLettersNormalize_v9.isMultiCornerStroke(6, beat, visit))
        val touch = HeatmapSwipeStrokeTouchSet_v4.Result(
            touched = visit.toSet(),
            counts = emptyMap(),
            orderedLetters = visit,
            rejectedTouchLetters = emptySet(),
            startLabel = "f",
            liftLabel = "g",
        )
        val normalized = HeatmapPathLettersNormalize_v9.normalize(
            rawBeatPath = beat,
            rawBeats = HeatmapSwipeBeat_v2.Result(6, emptyList()),
            neighborGraph = null,
            layout = flyingLayout(),
            pointers = emptyPointers(),
            touch = touch,
        )
        assertEquals(visit, normalized.letters)
    }

    private fun flyingLayout(): helium314.keyboard.heatmap.learning.HeatmapCoordinateMap_v1.Snapshot =
        helium314.keyboard.heatmap.learning.HeatmapCoordinateMap_v1.Snapshot(
            localeTag = "en",
            layoutSetExtra = "",
            mainLayoutName = "qwerty",
            elementId = 1,
            keyboardWidth = 1080,
            keyboardHeight = 600,
            layoutHash = "test-flying",
            keys = listOf(
                key("f", 0, 100),
                key("l", 700, 100),
                key("y", 500, 0),
                key("i", 700, 0),
                key("n", 500, 200),
                key("g", 100, 100),
            ),
        )

    private fun key(label: String, left: Int, top: Int) =
        helium314.keyboard.heatmap.learning.HeatmapCoordinateMap_v1.KeyBoundsEntry(
            label, 102, left, top, left + 80, top + 80,
        )

    private fun emptyPointers(): helium314.keyboard.latin.common.InputPointers =
        helium314.keyboard.latin.common.InputPointers(1).also {
            it.addPointer(0, 0, 0, 0)
        }
}
