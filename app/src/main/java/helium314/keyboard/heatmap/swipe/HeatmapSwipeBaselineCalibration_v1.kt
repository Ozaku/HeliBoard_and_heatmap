// SPDX-License-Identifier: GPL-3.0-only

// ai-note: Phase1 step 5.2 — offline ingest helpers for baseline calibration from export JSON

package helium314.keyboard.heatmap.swipe

import org.json.JSONArray
import org.json.JSONObject

object HeatmapSwipeBaselineCalibration_v1 {

    data class SwipeSample(
        val targetWord: String?,
        val swipeStyle: String?,
        val committedText: String,
        val intentPath: String?,
        val visitOrder: String?,
        val outcomeCorrect: Boolean?,
        val avgSpeedKeyWidthsPerSec: Double?,
        val isSlowStroke: Boolean?,
        val dwellDurationsMs: List<Long>,
        val tuningRevision: Int,
    )

    data class ClusterSummary(
        val targetWord: String,
        val swipeStyle: String,
        val sampleCount: Int,
        val medianDwellMs: Long,
        val medianAvgSpeedKwPerSec: Double,
        val slowStrokeRatio: Double,
        val intentPathLenMedian: Int,
        val visitOrderLenMedian: Int,
    )

    data class Report(
        val clusters: List<ClusterSummary>,
        val recommendedDwellMinMs: Long,
        val recommendedTransitSpeedKwPerSec: Double,
        val recommendedSlowStrokeKwPerSec: Double,
    )

    @JvmStatic
    fun parseExportJson(json: String): List<SwipeSample> {
        val root = JSONObject(json)
        val out = ArrayList<SwipeSample>()
        if (root.has("swipeTraceRing")) {
            parseRing(root.getJSONArray("swipeTraceRing"), out)
        }
        if (root.has("wordSessions")) {
            parseSessions(root.getJSONArray("wordSessions"), out)
        }
        return out
    }

    @JvmStatic
    fun cluster(samples: List<SwipeSample>): Report {
        val grouped = samples.groupBy { s ->
            val word = s.targetWord?.lowercase().orEmpty().ifEmpty { "unknown" }
            val style = s.swipeStyle.orEmpty().ifEmpty { "unspecified" }
            word to style
        }
        val clusters = grouped.map { (key, group) ->
            val dwells = group.flatMap { it.dwellDurationsMs }.sorted()
            val speeds = group.mapNotNull { it.avgSpeedKeyWidthsPerSec }.sorted()
            val intentLens = group.mapNotNull { it.intentPath?.length }.sorted()
            val visitLens = group.mapNotNull { it.visitOrder?.length }.sorted()
            ClusterSummary(
                targetWord = key.first,
                swipeStyle = key.second,
                sampleCount = group.size,
                medianDwellMs = medianLong(dwells),
                medianAvgSpeedKwPerSec = medianDouble(speeds),
                slowStrokeRatio = group.count { it.isSlowStroke == true }.toDouble() / group.size.coerceAtLeast(1),
                intentPathLenMedian = medianInt(intentLens),
                visitOrderLenMedian = medianInt(visitLens),
            )
        }.sortedByDescending { it.sampleCount }

        val allDwells = samples.flatMap { it.dwellDurationsMs }.sorted()
        val allSpeeds = samples.mapNotNull { it.avgSpeedKeyWidthsPerSec }.sorted()
        return Report(
            clusters = clusters,
            recommendedDwellMinMs = medianLong(allDwells).coerceIn(300L, 800L),
            recommendedTransitSpeedKwPerSec = (medianDouble(allSpeeds) * 1.4).coerceIn(3.5, 6.5),
            recommendedSlowStrokeKwPerSec = medianDouble(allSpeeds).coerceIn(2.0, 4.0),
        )
    }

    private fun parseRing(arr: JSONArray, out: MutableList<SwipeSample>) {
        for (i in 0 until arr.length()) {
            val o = arr.getJSONObject(i)
            out.add(
                SwipeSample(
                    targetWord = o.optString("swipeTargetWord").ifEmpty { null },
                    swipeStyle = o.optString("swipeStyle").ifEmpty { null },
                    committedText = o.optString("committedText"),
                    intentPath = o.optString("swipeIntentPath").ifEmpty { null },
                    visitOrder = o.optString("swipeVisitOrder").ifEmpty { null },
                    outcomeCorrect = if (o.has("outcomeCorrect")) o.optBoolean("outcomeCorrect") else null,
                    avgSpeedKeyWidthsPerSec = null,
                    isSlowStroke = null,
                    dwellDurationsMs = emptyList(),
                    tuningRevision = o.optInt("tuningRevision", 0),
                ),
            )
        }
    }

    private fun parseSessions(arr: JSONArray, out: MutableList<SwipeSample>) {
        for (i in 0 until arr.length()) {
            val o = arr.getJSONObject(i)
            if (o.optString("inputMode") != "SWIPE") continue
            val diag = o.optJSONObject("swipeDecodeDiagnostics")
            val dwellArr = o.optJSONArray("swipeDwellSegments")
            val dwellMs = ArrayList<Long>()
            if (dwellArr != null) {
                for (j in 0 until dwellArr.length()) {
                    dwellMs.add(dwellArr.getJSONObject(j).optLong("durationMs"))
                }
            }
            out.add(
                SwipeSample(
                    targetWord = o.optString("swipeTargetWord").ifEmpty { null },
                    swipeStyle = o.optString("swipeStyle").ifEmpty { null },
                    committedText = o.optString("committedText"),
                    intentPath = o.optString("swipeIntentPath").ifEmpty { null },
                    visitOrder = o.optString("swipeVisitOrder").ifEmpty { null },
                    outcomeCorrect = if (o.has("swipeOutcomeCorrect")) o.optBoolean("swipeOutcomeCorrect") else null,
                    avgSpeedKeyWidthsPerSec = diag?.optDouble("avgSpeedKeyWidthsPerSec"),
                    isSlowStroke = diag?.optBoolean("isSlowStroke"),
                    dwellDurationsMs = dwellMs,
                    tuningRevision = diag?.optInt("tuningRevision") ?: 0,
                ),
            )
        }
    }

    private fun medianLong(values: List<Long>): Long {
        if (values.isEmpty()) return HeatmapSwipeTuningConstants_v1.DWELL_MIN_MS
        return values[values.size / 2]
    }

    private fun medianDouble(values: List<Double>): Double {
        if (values.isEmpty()) return HeatmapSwipeIntentPrototype_v1.slowStrokeAvgKeyWidthsPerSec
        return values[values.size / 2]
    }

    private fun medianInt(values: List<Int>): Int {
        if (values.isEmpty()) return 0
        return values[values.size / 2]
    }
}
