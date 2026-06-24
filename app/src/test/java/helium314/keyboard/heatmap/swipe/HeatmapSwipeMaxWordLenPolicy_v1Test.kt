// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.heatmap.swipe

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class HeatmapSwipeMaxWordLenPolicy_v1Test {

    @Test
    fun threeKeyPathAllowsFourLetterWords() {
        assertEquals(5, HeatmapSwipeMaxWordLenPolicy_v1.maxDictLen(3, 3, 3))
    }

    @Test
    fun multiTouchRequiresMinTwoLetterOutput() {
        assertEquals(2, HeatmapSwipeMaxWordLenPolicy_v1.minOutputLen(3, 1, 1))
    }
}
