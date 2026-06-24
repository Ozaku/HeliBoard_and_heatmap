// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.heatmap.learning

import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class HeatmapInstrumentationJson_v1Test {
    @Test
    fun buildContainsSchemaAndWordSession() {
        val ctx = ApplicationProvider.getApplicationContext<android.content.Context>()
        val session = WordSession_v2(
            slotId = WordSlotId_v1(1),
            sessionGeneration = 2L,
            hostPackage = "com.example",
            inputMode = WordSessionInputMode_v1.TAP,
            commitType = WordSessionCommitType_v1.DECIDED,
            committedText = "hello",
            finalText = "hello",
            typedText = "hel",
            separatorCharCount = 1,
            committedAtMs = 1000L,
        )
        val json = HeatmapInstrumentationJson_v1.build(
            context = ctx,
            sessions = listOf(session),
            journalEntries = listOf(ParagraphJournal_v1.Entry(WordSlotId_v1(1), "hello", 6)),
            liveSummary = null,
            source = "unit_test",
        )
        assertTrue(json.contains("\"schemaVersion\": 1"))
        assertTrue(json.contains("\"committedText\": \"hello\""))
        assertTrue(json.contains("\"exportSource\": \"unit_test\""))
    }
}
