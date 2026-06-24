// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.heatmap.learning

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class HeatmapCoordinateMap_v1Test {
    @Test
    fun layoutHashStableForSameGeometry() {
        val keys = listOf(
            HeatmapCoordinateMap_v1.KeyBoundsEntry("Q", 113, 0, 150, 50, 0),
            HeatmapCoordinateMap_v1.KeyBoundsEntry("W", 119, 150, 0, 190, 50),
        )
        val a = HeatmapCoordinateMap_v1.computeLayoutHash("en-US", "MAIN=qwerty", 0, 1080, 600, keys)
        val b = HeatmapCoordinateMap_v1.computeLayoutHash("en-US", "MAIN=qwerty", 0, 1080, 600, keys)
        assertEquals(a, b)
        assertTrue(a.startsWith("lh_"))
    }

    @Test
    fun layoutHashChangesWhenKeyMoves() {
        val keys1 = listOf(HeatmapCoordinateMap_v1.KeyBoundsEntry("I", 105, 100, 100, 130, 150))
        val keys2 = listOf(HeatmapCoordinateMap_v1.KeyBoundsEntry("I", 105, 200, 100, 230, 150))
        val h1 = HeatmapCoordinateMap_v1.computeLayoutHash("en-US", "MAIN=qwerty", 0, 1080, 600, keys1)
        val h2 = HeatmapCoordinateMap_v1.computeLayoutHash("en-US", "MAIN=qwerty", 0, 1080, 600, keys2)
        assertNotEquals(h1, h2)
    }

    @Test
    fun keyLabelForFirstLetterUsesMap() {
        val snap = HeatmapCoordinateMap_v1.Snapshot(
            localeTag = "en-US",
            layoutSetExtra = "MAIN=qwerty",
            mainLayoutName = "qwerty",
            elementId = 0,
            keyboardWidth = 100,
            keyboardHeight = 100,
            layoutHash = "lh_test",
            keys = listOf(HeatmapCoordinateMap_v1.KeyBoundsEntry("T", 116, 0, 0, 10, 10)),
        )
        assertEquals("t", snap.keyLabelForFirstLetter("testing"))
    }
}
