// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.heatmap.learning

import android.text.InputType
import android.view.inputmethod.EditorInfo
import androidx.test.core.app.ApplicationProvider
import helium314.keyboard.latin.InputAttributes
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class HeatmapLearningGate_v1Test {
    @Test
    fun blocksPasswordInputType() {
        val info = EditorInfo().apply {
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
            packageName = "com.example"
        }
        val attrs = InputAttributes(info, false, "com.example")
        assertFalse(HeatmapLearningGate_v1.shouldRecord(attrs))
    }

    @Test
    fun allowsNormalTextWhenLearningOn() {
        val ctx = ApplicationProvider.getApplicationContext<android.content.Context>()
        val info = EditorInfo().apply {
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_NORMAL
            packageName = "com.example"
        }
        assertTrue(HeatmapLearningGate_v1.shouldRecord(ctx, info))
    }
}
