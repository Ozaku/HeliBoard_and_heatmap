// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.heatmap.swipe

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class HeatmapSwipeLengthBand_v1Test {

    @Test
    fun exactLengthFullMultiplier() {
        assertEquals(1.0, HeatmapSwipeLengthBand_v1.multiplier(6, 6), 0.0)
    }

    @Test
    fun twoLetterShorterStrongPenalty() {
        assertTrue(HeatmapSwipeLengthBand_v1.multiplier(4, 6) < 0.5)
    }
}
