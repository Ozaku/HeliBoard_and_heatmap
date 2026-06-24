// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.heatmap.swipe

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class HeatmapSwipeWordTouchGate_v1Test {

    private val touchedFet = setOf("f", "e", "t")

    @Test
    fun rejectsFatWhenANotTouched() {
        assertFalse(HeatmapSwipeWordTouchGate_v1.isAllowed("fat", touchedFet, setOf('e')))
        assertFalse(HeatmapSwipeWordTouchGate_v1.isAllowed("fart", touchedFet, emptySet()))
    }

    @Test
    fun allowsFeetWithDoubleOnTouchedE() {
        assertTrue(HeatmapSwipeWordTouchGate_v1.isAllowed("feet", touchedFet, setOf('e')))
        assertTrue(HeatmapSwipeWordTouchGate_v1.isAllowed("feet", touchedFet, emptySet()))
    }
}
