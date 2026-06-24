// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.heatmap.swipe

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class HeatmapSwipeTapGate_v1Test {
    @Test
    fun microDragBelowFallbackKeySizeIsNotSwipe() {
        assertFalse(
            HeatmapSwipeTapGate_v1.qualifiesAsSwipe(
                startX = 100, startY = 100,
                currentX = 110, currentY = 105,
                startKey = null,
                fallbackKeyWidth = 50,
                fallbackKeyHeight = 50,
            ),
        )
    }

    @Test
    fun horizontalSpanAtLeastKeyWidthQualifies() {
        assertTrue(
            HeatmapSwipeTapGate_v1.qualifiesAsSwipe(
                startX = 100, startY = 100,
                currentX = 155, currentY = 102,
                startKey = null,
                fallbackKeyWidth = 50,
                fallbackKeyHeight = 50,
            ),
        )
    }

    @Test
    fun verticalSpanAtLeastKeyHeightQualifies() {
        assertTrue(
            HeatmapSwipeTapGate_v1.qualifiesAsSwipe(
                startX = 100, startY = 100,
                currentX = 102, currentY = 160,
                startKey = null,
                fallbackKeyWidth = 50,
                fallbackKeyHeight = 50,
            ),
        )
    }
}
