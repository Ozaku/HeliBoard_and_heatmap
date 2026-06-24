// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.heatmap.swipe

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class HeatmapSwipeStrokeMonotonicPath_v1Test {

    @Test
    fun acceptsOrderedSubsequence() {
        val path = listOf("t", "e", "s", "t")
        assertTrue(HeatmapSwipeStrokeMonotonicPath_v1.isMonotonicSubsequence("test", path))
        assertTrue(HeatmapSwipeStrokeMonotonicPath_v1.isMonotonicSubsequence("tes", path))
    }

    @Test
    fun rejectsOutOfOrderLetters() {
        val path = listOf("t", "e", "s", "t")
        assertFalse(HeatmapSwipeStrokeMonotonicPath_v1.isMonotonicSubsequence("tset", path))
        assertFalse(HeatmapSwipeStrokeMonotonicPath_v1.isMonotonicSubsequence("trews", path))
    }

    @Test
    fun progressivePrefixesStayInOrder() {
        val prefixes = HeatmapSwipeStrokeMonotonicPath_v1.progressivePrefixes(
            listOf("t", "e", "s", "t"), 24,
        )
        assertTrue(prefixes.contains("test"))
        assertTrue(prefixes.contains("tes"))
        assertFalse(prefixes.contains("tset"))
    }

    @Test
    fun filterPrefixesRejectsShuffledVariants() {
        val ordered = listOf("t", "e", "s", "t")
        val filtered = HeatmapSwipeStrokeMonotonicPath_v1.filterPrefixes(
            listOf("test", "tset", "tes", "trews"),
            ordered,
        )
        assertEquals(listOf("test", "tes"), filtered.sortedByDescending { it.length })
    }
}