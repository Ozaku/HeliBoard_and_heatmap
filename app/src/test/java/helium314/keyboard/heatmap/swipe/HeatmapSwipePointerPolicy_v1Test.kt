// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.heatmap.swipe

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class HeatmapSwipePointerPolicy_v1Test {

    @Test
    fun strokeGateMatchesTapGate() {
        assertFalse(
            HeatmapSwipePointerPolicy_v1.strokeQualifiesAsSwipe(
                0, 0, 0, 0, null, null,
            ),
        )
        assertTrue(
            HeatmapSwipePointerPolicy_v1.strokeQualifiesAsSwipe(
                0, 0, 50, 0, null, null,
            ),
        )
    }
}
