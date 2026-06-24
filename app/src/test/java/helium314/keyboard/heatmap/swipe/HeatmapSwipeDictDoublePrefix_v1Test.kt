// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.heatmap.swipe

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class HeatmapSwipeDictDoublePrefix_v1Test {

    @Test
    fun footPrefixFromFotPath() {
        val variants = HeatmapSwipeDictDoublePrefix_v1.prefixVariants(listOf("f", "o", "t"), 8)
        assertTrue(variants.contains("foot"))
    }

    @Test
    fun feetPrefixFromFetPath() {
        val variants = HeatmapSwipeDictDoublePrefix_v1.prefixVariants(listOf("f", "e", "t"), 8)
        assertTrue(variants.contains("feet"))
    }
}
