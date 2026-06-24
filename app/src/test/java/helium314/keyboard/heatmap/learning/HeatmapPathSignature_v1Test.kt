// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.heatmap.learning

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class HeatmapPathSignature_v1Test {
    @Test
    fun signatureStableForSameStroke() {
        val xs = intArrayOf(100, 200, 300, 400)
        val ys = intArrayOf(50, 60, 70, 80)
        val a = HeatmapPathSignature_v1.compute(xs, ys, 4, 1080, 600)
        val b = HeatmapPathSignature_v1.compute(xs, ys, 4, 1080, 600)
        assertEquals(a?.signatureHash, b?.signatureHash)
        assertTrue(a!!.signatureHash.startsWith("ps_"))
        assertEquals(HeatmapPathSignature_v1.POLYLINE_BYTES, a.polylineBlob.size)
    }

    @Test
    fun signatureChangesWhenStrokeChanges() {
        val xs1 = intArrayOf(100, 200, 300)
        val ys1 = intArrayOf(50, 60, 70)
        val xs2 = intArrayOf(100, 250, 300)
        val ys2 = intArrayOf(50, 60, 70)
        val h1 = HeatmapPathSignature_v1.compute(xs1, ys1, 3, 1080, 600)?.signatureHash
        val h2 = HeatmapPathSignature_v1.compute(xs2, ys2, 3, 1080, 600)?.signatureHash
        assertNotEquals(h1, h2)
    }

    @Test
    fun needsAtLeastTwoPoints() {
        val xs = intArrayOf(100)
        val ys = intArrayOf(50)
        assertEquals(null, HeatmapPathSignature_v1.compute(xs, ys, 1, 1080, 600))
    }
}
