// SPDX-License-Identifier: GPL-3.0-only

package helium314.keyboard.heatmap.swipe

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class HeatmapSwipeStartLetterSoftAnchor_v1Test {

    @Test
    fun blocksOilsWhenStartIsW() {
        val dist = listOf(
            HeatmapKeyLikelihood_v6.LabelWeight("w", 0.65),
            HeatmapKeyLikelihood_v6.LabelWeight("e", 0.35),
        )
        val graph = HeatmapKeyNeighborGraph_v2.staticQwerty()
        assertFalse(
            HeatmapSwipeStartLetterSoftAnchor_v1.wordAllowedAtStart("oils", dist, graph),
        )
        assertTrue(
            HeatmapSwipeStartLetterSoftAnchor_v1.wordAllowedAtStart("works", dist, graph),
        )
    }

    @Test
    fun prefixFanOutIncludesPrimaryStart() {
        val dist = listOf(HeatmapKeyLikelihood_v6.LabelWeight("w", 0.7))
        val prefixes = HeatmapSwipeStartLetterSoftAnchor_v1.prefixVariantsFromStart(
            listOf("w", "o", "r"), dist, maxLen = 8,
        )
        assertTrue(prefixes.any { it.startsWith("w") })
    }
}
