// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.heatmap.swipe

import org.junit.Assert.assertEquals
import org.junit.Test

class HeatmapSwipeMaxWordLenPolicy_v3Test {

    @Test
    fun longPathDoesNotForceMinLenToFullPath() {
        assertEquals(2, HeatmapSwipeMaxWordLenPolicy_v3.minOutputLen(7, 7, 7, 8))
    }

    @Test
    fun shortWiggleStillBoostsMinLen() {
        assertEquals(4, HeatmapSwipeMaxWordLenPolicy_v3.minOutputLen(3, 3, 3, 6))
    }
}
