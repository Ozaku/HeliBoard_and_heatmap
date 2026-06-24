// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.heatmap.swipe

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class HeatmapSwipeStartLetterAnchor_v1Test {

    @Test
    fun anchorReplacesWrongFirstLetter() {
        val anchored = HeatmapSwipeStartLetterAnchor_v1.anchorPathLetters(
            listOf("o", "i", "l", "s"),
            startLabel = "w",
            touchedLetters = setOf("w", "o", "i", "l", "s"),
        )
        assertEquals(listOf("w", "i", "l", "s"), anchored)
    }

    @Test
    fun prefixesFilteredToStart() {
        val filtered = HeatmapSwipeStartLetterAnchor_v1.filterPrefixes(
            listOf("oils", "works", "worms", "wo"),
            startLabel = "w",
        )
        assertTrue(filtered.all { it.startsWith("w") })
        assertFalse(filtered.any { it.startsWith("o") })
    }

    @Test
    fun oilsRejectedWhenStartIsW() {
        assertFalse(
            HeatmapSwipeStartLetterAnchor_v1.wordStartsWithAnchor("oils", "w"),
        )
        assertTrue(
            HeatmapSwipeStartLetterAnchor_v1.wordStartsWithAnchor("works", "w"),
        )
    }

    @Test
    fun hopFromWToOIsUnreachable() {
        val hops = HeatmapSwipeStartLetterAnchor_v1.hopDistance(
            HeatmapKeyNeighborGraph_v2.staticQwerty(),
            "w",
            "o",
        )
        assertTrue(hops > HeatmapSwipeStartLetterAnchor_v1.hopDistance(
            HeatmapKeyNeighborGraph_v2.staticQwerty(), "w", "e",
        ))
        assertFalse(
            HeatmapSwipeStartLetterAnchor_v1.isEarlyLetterReachable(
                HeatmapKeyNeighborGraph_v2.staticQwerty(), "w", "o", 0,
            ),
        )
    }

    @Test
    fun wordTouchGateV2BlocksOilsForStartW() {
        val touched = setOf("w", "o", "r", "k", "s", "i", "l")
        assertFalse(
            HeatmapSwipeWordTouchGate_v2.isAllowed("oils", touched, "w"),
        )
        assertTrue(
            HeatmapSwipeWordTouchGate_v2.isAllowed("works", touched, "w"),
        )
    }
}
