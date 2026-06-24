// SPDX-License-Identifier: GPL-3.0-only

package helium314.keyboard.heatmap.swipe

import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class HeatmapSwipeBaselineCalibration_v1Test {

    @Test
    fun clustersExportSamples() {
        val json = """
            {
              "schemaVersion": 2,
              "swipeTraceRing": [
                {
                  "committedText": "works",
                  "swipeIntentPath": "works",
                  "swipeVisitOrder": "works",
                  "swipeTargetWord": "works",
                  "swipeStyle": "slow_deliberate",
                  "outcomeCorrect": true,
                  "tuningRevision": 1
                }
              ],
              "wordSessions": []
            }
        """.trimIndent()
        val samples = HeatmapSwipeBaselineCalibration_v1.parseExportJson(json)
        assertTrue(samples.isNotEmpty())
        val report = HeatmapSwipeBaselineCalibration_v1.cluster(samples)
        assertTrue(report.clusters.any { it.targetWord == "works" })
    }
}
