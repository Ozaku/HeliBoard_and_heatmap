// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.heatmap.swipe

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class HeatmapSwipeDictDoublePrefix_v3Test {

    @Test
    fun doublesOnlyHintedIndex() {
        val path = listOf("t", "y", "p", "i", "n", "g")
        val none = HeatmapSwipeDictDoublePrefix_v3.prefixVariantsAtIndices(path, 10, emptySet())
        assertTrue(none.isEmpty())
        val one = HeatmapSwipeDictDoublePrefix_v3.prefixVariantsAtIndices(path, 10, setOf(1))
        assertEquals(listOf("tyyping"), one)
    }

    @Test
    fun feetFromFetHintAtE() {
        val path = listOf("f", "e", "t")
        val variants = HeatmapSwipeDictDoublePrefix_v3.prefixVariantsAtIndices(path, 8, setOf(1))
        assertEquals(listOf("feet"), variants)
    }
}
