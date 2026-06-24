// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.heatmap.swipe

import org.junit.Assert.assertTrue
import org.junit.Test

class HeatmapSwipeDwellDoubleLetter_v1Test {

    @Test
    fun expandsFeetPathFromFetDwell() {
        val normalized = HeatmapPathLettersNormalize_v2.normalize(
            listOf("f", "e", "e", "e", "t"),
            HeatmapKeyNeighborGraph_v2.staticQwerty(),
        )
        val paths = HeatmapSwipeDwellDoubleLetter_v1.expandPaths(normalized)
        assertTrue(paths.any { it.joinToString("") == "feet" })
    }
}
