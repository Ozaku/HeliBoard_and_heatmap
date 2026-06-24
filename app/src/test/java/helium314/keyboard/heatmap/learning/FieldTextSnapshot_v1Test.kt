// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.heatmap.learning

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class FieldTextSnapshot_v1Test {
    @Test
    fun fingerprintStableForSameText() {
        val a = FieldTextSnapshot_v1(beforeChars = 3, afterChars = 2, fingerprint = "hel|lo".hashCode())
        val b = FieldTextSnapshot_v1(beforeChars = 3, afterChars = 2, fingerprint = "hel|lo".hashCode())
        assertEquals(a.fingerprint, b.fingerprint)
        assertFalse(a.skippedReason != null)
    }

    @Test
    fun skippedProbeNotProbed() {
        val s = FieldTextSnapshot_v1(skippedReason = "no_personalized")
        assertEquals("skip=no_personalized", s.debugToken())
    }
}
