// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.heatmap.swipe

import org.junit.Assert.assertEquals
import org.junit.Test

class HeatmapSwipeMaxWordLenPolicy_v2Test {

    @Test
    fun fourLetterPathRequiresFourLetterOutput() {
        assertEquals(4, HeatmapSwipeMaxWordLenPolicy_v2.minOutputLen(3, 4, 4, 6))
    }

    @Test
    fun wiggleRawBeatsRaiseMinLenWhenPathStillThree() {
        assertEquals(4, HeatmapSwipeMaxWordLenPolicy_v2.minOutputLen(3, 3, 3, 6))
    }

    @Test
    fun plainThreeKeyStrokeKeepsMinTwo() {
        assertEquals(2, HeatmapSwipeMaxWordLenPolicy_v2.minOutputLen(3, 3, 3, 3))
    }
}
