// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.heatmap.swipe

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class HeatmapSwipeDoublePrefixHint_v1Test {

    @Test
    fun threeLetterHighBeatsAddsMiddleHint() {
        val hints = HeatmapSwipeDoublePrefixHint_v1.hintIndices(
            listOf("f", "e", "t"),
            beatCountRaw = 5,
            wiggleHints = emptyList(),
        )
        assertEquals(setOf(1), hints)
    }

    @Test
    fun longPathWithoutWiggleHasNoHints() {
        val hints = HeatmapSwipeDoublePrefixHint_v1.hintIndices(
            listOf("f", "l", "y", "i", "n", "g"),
            beatCountRaw = 6,
            wiggleHints = emptyList(),
        )
        assertTrue(hints.isEmpty())
    }
}
