// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.heatmap.learning

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class HeatmapSwipeSingleLetterBan_v1Test {
    @Test
    fun onlyAandIAreBanned() {
        assertTrue(HeatmapSwipeSingleLetterBan_v1.isBannedSwipeSingleLetterWord("I"))
        assertTrue(HeatmapSwipeSingleLetterBan_v1.isBannedSwipeSingleLetterWord("a"))
        assertFalse(HeatmapSwipeSingleLetterBan_v1.isBannedSwipeSingleLetterWord("o"))
        assertFalse(HeatmapSwipeSingleLetterBan_v1.isBannedSwipeSingleLetterWord("it"))
        assertFalse(HeatmapSwipeSingleLetterBan_v1.isBannedSwipeSingleLetterWord("Aardvark"))
    }
}
