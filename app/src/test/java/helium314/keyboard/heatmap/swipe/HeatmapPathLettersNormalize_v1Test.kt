// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.heatmap.swipe

import org.junit.Assert.assertEquals
import org.junit.Test

class HeatmapPathLettersNormalize_v1Test {

    @Test
    fun collapsesConsecutiveDuplicates() {
        assertEquals(
            listOf("p", "l", "a", "t", "e", "s"),
            HeatmapPathLettersNormalize_v1.collapseConsecutiveDuplicates(
                listOf("p", "l", "a", "t", "t", "e", "s", "s"),
            ),
        )
        assertEquals(
            listOf("t", "h", "e", "r", "e"),
            HeatmapPathLettersNormalize_v1.collapseConsecutiveDuplicates(
                listOf("t", "h", "e", "e", "r", "e"),
            ),
        )
    }
}
