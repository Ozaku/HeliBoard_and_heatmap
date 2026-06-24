// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.heatmap.swipe

import org.junit.Assert.assertEquals
import org.junit.Test

class HeatmapSwipeMaxWordLenPolicy_v4Test {

    @Test
    fun fourCornerPathRequiresFourLetterMinimum() {
        assertEquals(4, HeatmapSwipeMaxWordLenPolicy_v4.minOutputLen(4, 8, 15))
    }

    @Test
    fun threeCornerPathRequiresThreeLetters() {
        assertEquals(3, HeatmapSwipeMaxWordLenPolicy_v4.minOutputLen(3, 5, 10))
    }
}