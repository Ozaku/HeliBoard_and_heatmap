// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.heatmap.learning

import org.junit.Assert.assertEquals
import org.junit.Test

class HeatmapMetricsRecorder_v1Test {
    @Test
    fun percentileFromRingString() {
        val encoded = "1,5,10,20,100"
        val p95 = encoded.split(',').map { it.toLong() }.sorted().let { arr ->
            val rank = ((95 / 100.0) * (arr.size - 1)).toInt()
            arr[rank]
        }
        assertEquals(100L, p95)
    }
}
