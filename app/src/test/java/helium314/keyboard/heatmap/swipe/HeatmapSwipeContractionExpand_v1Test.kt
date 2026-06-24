// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.heatmap.swipe

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class HeatmapSwipeContractionExpand_v1Test {

    @Test
    fun expandsImToImApostrophe() {
        assertEquals(listOf("I'm"), HeatmapSwipeContractionExpand_v1.expansions("im"))
    }

    @Test
    fun expandsIsntToIsNot() {
        assertTrue(HeatmapSwipeContractionExpand_v1.expansions("isnt").contains("isn't"))
    }
}
