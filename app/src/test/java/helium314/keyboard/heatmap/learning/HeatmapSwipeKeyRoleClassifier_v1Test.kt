// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.heatmap.learning

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class HeatmapSwipeKeyRoleClassifier_v1Test {

    @Test
    fun firstAndLastRolesForThreeLetterWord() {
        assertEquals(
            HeatmapSwipeGeometryVector_v1.KeyRole.FIRST,
            HeatmapSwipeKeyRoleClassifier_v1.roleForLabel("cat", "c"),
        )
        assertEquals(
            HeatmapSwipeGeometryVector_v1.KeyRole.LAST,
            HeatmapSwipeKeyRoleClassifier_v1.roleForLabel("cat", "t"),
        )
        assertEquals(
            HeatmapSwipeGeometryVector_v1.KeyRole.IN_WORD,
            HeatmapSwipeKeyRoleClassifier_v1.roleForLabel("cat", "a"),
        )
    }

    @Test
    fun extraRoleWhenLabelNotInWord() {
        assertEquals(
            HeatmapSwipeGeometryVector_v1.KeyRole.EXTRA,
            HeatmapSwipeKeyRoleClassifier_v1.roleForLabel("cat", "z"),
        )
    }

    @Test
    fun gapRoleWhenLabelMissing() {
        assertEquals(
            HeatmapSwipeGeometryVector_v1.KeyRole.GAP,
            HeatmapSwipeKeyRoleClassifier_v1.roleForLabel("cat", null),
        )
    }

    @Test
    fun middleLetterDetection() {
        assertTrue(HeatmapSwipeKeyRoleClassifier_v1.isMiddleLetter("hello", "l"))
        assertFalse(HeatmapSwipeKeyRoleClassifier_v1.isMiddleLetter("hi", "h"))
    }
}
