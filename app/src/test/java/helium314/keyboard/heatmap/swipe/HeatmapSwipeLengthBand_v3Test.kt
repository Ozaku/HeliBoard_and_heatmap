// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.heatmap.swipe

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class HeatmapSwipeLengthBand_v3Test {

    @Test
    fun visitAwareRefLenSoftensPenaltyForLongCandidates() {
        val v2 = HeatmapSwipeLengthBand_v2.multiplier(8, 4)
        val v3 = HeatmapSwipeLengthBand_v3.multiplier(8, 4, visitLetterCount = 8)
        assertTrue(v3 > v2)
    }

    @Test
    fun equalLengthsUnchangedFromV2() {
        assertEquals(
            HeatmapSwipeLengthBand_v2.multiplier(4, 4),
            HeatmapSwipeLengthBand_v3.multiplier(4, 4, visitLetterCount = 4),
            0.001,
        )
    }
}

