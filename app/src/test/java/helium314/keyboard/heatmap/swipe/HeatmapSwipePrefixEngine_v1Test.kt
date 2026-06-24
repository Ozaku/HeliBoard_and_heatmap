// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.heatmap.swipe

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class HeatmapSwipePrefixEngine_v1Test {

    @Test
    fun buildsLongestPrefixFirst() {
        val infer = HeatmapSwipeSegmentInfer_v2.Result(
            startKeyLabel = "c",
            pathLetters = listOf("c", "o", "m", "p", "a", "s", "s"),
            endKeyLabel = "s",
            beatCount = 7,
            classifiedBeats = emptyList(),
        )
        val variants = HeatmapSwipePrefixEngine_v1.buildPrefixVariants(infer)
        assertEquals("compass", variants.first())
        assertTrue(variants.contains("co"))
        assertTrue(variants.contains("c"))
    }
}
