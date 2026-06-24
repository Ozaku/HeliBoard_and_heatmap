// SPDX-License-Identifier: GPL-3.0-only

// ai-note: v4 — swipe ring v3 + tuning revision 3 export wiring with swipe geometry + correction chains

package helium314.keyboard.heatmap.learning

import android.content.Context
import android.content.SharedPreferences
import org.json.JSONArray
import org.json.JSONObject

object HeatmapInstrumentationJson_v4 {

    const val SCHEMA_VERSION = 4

    fun build(
        context: Context,
        wordMemory: List<WordSession_v5>,
        sessions: List<WordSession_v5>,
        journalEntries: List<ParagraphJournal_v2.Entry>,
        liveSummary: HeatmapInstrumentationSnapshot_v6.LiveSummary?,
        source: String,
    ): String {
        val prefs = HeatmapCrossProcessPrefs_v2.readPrefs(context)
        val root = JSONObject()
        root.put("schemaVersion", SCHEMA_VERSION)
        root.put("exportedAtMs", System.currentTimeMillis())
        root.put("exportSource", source)
        root.put("build", buildInfo())
        root.put("settings", settingsBlock(context))
        root.put("wordMemoryStats", wordMemoryStatsBlock())
        root.put("instrumentation", instrumentationBlock(prefs))
        root.put("metrics", metricsBlock(prefs))
        root.put("learningStore", learningStoreBlock(context))
        root.put(
            "wordMemory",
            JSONArray().apply { wordMemory.forEach { put(sessionToJson(it)) } },
        )
        root.put("wordSessions", JSONArray().apply { sessions.forEach { put(sessionToJson(it)) } })
        root.put("swipeTraceRing", swipeRingBlock())
        root.put("swipeCorrectionChains", swipeCorrectionChainsBlock())
        root.put("paragraphJournal", JSONArray().apply { journalEntries.forEach { put(journalEntryToJson(it)) } })
        liveSummary?.let { root.put("liveImeSession", liveSummaryToJson(it)) }
        return root.toString(2)
    }

    private fun wordMemoryStatsBlock(): JSONObject = JSONObject().apply {
        put("maxWordEntries", HeatmapWordMemoryLedger_v2.maxWordEntries())
        put("maxCharBudget", HeatmapWordMemoryLedger_v2.maxCharBudget())
        put("entryCount", HeatmapWordMemoryLedger_v2.entryCount())
        put("usedCharBudget", HeatmapWordMemoryLedger_v2.usedCharBudget())
        put("keptCount", HeatmapWordMemoryLedger_v2.keptCount())
        put("erasedCount", HeatmapWordMemoryLedger_v2.erasedCount())
    }

    private fun buildInfo(): JSONObject = JSONObject().apply {
        put("beta", HeatmapLearningBuildInfo_v1.BETA_VERSION)
        put("roadmapStep", HeatmapLearningBuildInfo_v1.ROADMAP_STEP)
        put("statusLine", HeatmapLearningBuildInfo_v1.statusLine())
        put("swipeTuningRevision", helium314.keyboard.heatmap.swipe.HeatmapSwipeTuningConstants_v4.TUNING_REVISION)
    }

    private fun settingsBlock(context: Context): JSONObject = JSONObject().apply {
        put("learningEnabled", HeatmapLearningSettings_v2.isLearningEnabled(context))
        put("paragraphWindowChars", HeatmapLearningSettings_v2.getParagraphWindowChars(context))
        put("tapHeatmapAutocorrect", HeatmapAutocorrectSettings_v1.isTapHeatmapAutocorrectEnabled(context))
        put("swipeHeatmapAutocorrect", HeatmapAutocorrectSettings_v1.isSwipeHeatmapAutocorrectEnabled(context))
        put("autoFileDump", HeatmapExportSettings_v1.isAutoFileDumpEnabled(context))
        put("wordMemoryMaxWords", HeatmapLearningSettings_v1.WORD_MEMORY_MAX_WORDS)
    }

    private fun learningStoreBlock(context: Context): JSONObject {
        val s = HeatmapLearningStore_v1.readSnapshot(context)
        return JSONObject().apply {
            put("schemaVersion", HeatmapLearningDatabase_v1.SCHEMA_VERSION)
            put("dbPath", s.dbPath)
            put("persistedCommitCount", s.commitCount)
            put("keyStatRows", s.keyStatRows)
            put("wordPairRows", s.wordPairRows)
            put("layoutHash", s.layoutHash ?: HeatmapLayoutContext_v2.layoutHash(context))
            put("localeTag", HeatmapLayoutContext_v2.localeTag(context))
            put("keyBoundsRows", s.keyBoundsRows)
            put("pathBucketRows", s.pathBucketRows)
            put("alignmentOffsetRows", s.alignmentOffsetRows)
            put("letterConfusionRows", s.letterConfusionRows)
            put("wordCorrectionRows", s.wordCorrectionRows)
        }
    }

    private fun metricsBlock(prefs: SharedPreferences): JSONObject {
        val m = HeatmapMetricsRecorder_v1.readSnapshot(prefs)
        return JSONObject().apply {
            put("commitCount", m.commitCount)
            put("commitLastMs", m.commitLastMs)
            put("commitAvgMs", m.commitAvgMs)
            put("commitMaxMs", m.commitMaxMs)
            put("commitP95Ms", m.commitP95Ms)
            put("heapUsedMbAtFlush", m.heapUsedMb)
            put("heapMaxMbAtFlush", m.heapMaxMb)
            put("prefixBranchStubCount", m.prefixBranchStubCount)
        }
    }

    private fun instrumentationBlock(prefs: SharedPreferences): JSONObject = JSONObject().apply {
        put("lifetimeCommits", prefs.getInt("heatmap_total_commits_ever", 0))
        put("lastFlushReason", prefs.getString("heatmap_last_flush_reason", null))
        put("lastFlushHost", prefs.getString("heatmap_last_flush_host", null))
        put("totalFlushes", prefs.getInt("heatmap_total_flushes", 0))
        put("selectionEditEvents", prefs.getInt("heatmap_selection_edit_events", 0))
        put("pendingFinalTextSync", prefs.getInt("heatmap_pending_finaltext_sync", 0))
        put("lastFieldProbe", prefs.getString("heatmap_last_field_probe", null))
        put("snapshotBeta", prefs.getString("heatmap_snapshot_beta", null))
        put("snapshotJournalChars", prefs.getInt("heatmap_snapshot_journal_chars", 0))
        put("snapshotJournalWords", prefs.getInt("heatmap_snapshot_journal_words", 0))
        put("snapshotLastWord", prefs.getString("heatmap_snapshot_last_word", null))
        put("snapshotHostPackage", prefs.getString("heatmap_snapshot_host_package", null))
    }

    private fun swipeRingBlock(): JSONArray = JSONArray().apply {
        HeatmapSwipeTraceRingBuffer_v3.copyEntriesNewestFirst().forEach { e ->
            put(
                JSONObject().apply {
                    put("committedText", e.committedText)
                    put("swipeIntentPath", e.swipeIntentPath)
                    put("swipeVisitOrder", e.swipeVisitOrder)
                    put("swipeTargetWord", e.swipeTargetWord)
                    put("swipeStyle", e.swipeStyle)
                    put("wordMemoryOutcome", e.wordMemoryOutcome)
                    put("tracePointCount", e.tracePointCount)
                    put("geometrySegmentCount", e.geometrySegmentCount)
                    put("tuningRevision", e.tuningRevision)
                    put("committedAtMs", e.committedAtMs)
                },
            )
        }
    }

    private fun swipeCorrectionChainsBlock(): JSONArray = JSONArray().apply {
        HeatmapSwipeCorrectionChain_v1.copyResolvedOldestFirst().forEach { chain ->
            put(
                JSONObject().apply {
                    put("chainId", chain.chainId)
                    put("finalWord", chain.finalWord)
                    put(
                        "attempts",
                        JSONArray().apply {
                            chain.attempts.forEach { attempt ->
                                put(
                                    JSONObject().apply {
                                        put("memorySequence", attempt.memorySequence)
                                        put("attemptText", attempt.attemptText)
                                        put("outcome", attempt.outcome.name)
                                        attempt.geometry?.let { put("swipeGeometry", geometryToJson(it)) }
                                    },
                                )
                            }
                        },
                    )
                },
            )
        }
    }

    private fun sessionToJson(s: WordSession_v5): JSONObject = JSONObject().apply {
        put("slotId", s.slotId.value)
        put("sessionGeneration", s.sessionGeneration)
        put("hostPackage", s.hostPackage)
        put("inputMode", s.inputMode.name)
        put("commitType", s.commitType.name)
        put("wordMemoryOutcome", s.wordMemoryOutcome.name)
        put("memorySequence", s.memorySequence)
        s.linkedSlotId?.let { put("linkedSlotId", it.value) }
        put("committedText", s.committedText)
        put("finalText", s.finalText)
        put("typedText", s.typedText)
        put("separatorCharCount", s.separatorCharCount)
        put("committedAtMs", s.committedAtMs)
        put("swipeTracePointCount", s.swipeTracePointCount)
        put("tapKeyCount", s.tapKeyCount)
        if (s.runnerUpWords.isNotEmpty()) {
            put("runnerUpWords", JSONArray().apply { s.runnerUpWords.forEach { put(it) } })
        }
        s.swipeInferredPath?.let { put("swipeInferredPath", it) }
        s.swipeInferredPathRaw?.let { put("swipeInferredPathRaw", it) }
        s.swipeIntentPath?.let { put("swipeIntentPath", it) }
        s.swipeVisitOrder?.let { put("swipeVisitOrder", it) }
        if (s.swipeStartDistribution.isNotEmpty()) {
            put(
                "swipeStartDistribution",
                JSONArray().apply {
                    s.swipeStartDistribution.forEach { w ->
                        put(JSONObject().apply {
                            put("label", w.label)
                            put("likelihood", w.likelihood)
                        })
                    }
                },
            )
        }
        if (s.swipeDwellSegments.isNotEmpty()) {
            put(
                "swipeDwellSegments",
                JSONArray().apply {
                    s.swipeDwellSegments.forEach { d ->
                        put(
                            JSONObject().apply {
                                put("dominantLabel", d.dominantLabel)
                                put("durationMs", d.durationMs)
                                put("sampleCount", d.sampleCount)
                                put("startMs", d.startMs)
                                put("endMs", d.endMs)
                            },
                        )
                    }
                },
            )
        }
        if (s.swipeTracePoints.isNotEmpty()) {
            put(
                "swipeTracePoints",
                JSONArray().apply {
                    s.swipeTracePoints.forEach { p ->
                        put(
                            JSONObject().apply {
                                put("x", p.x)
                                put("y", p.y)
                                put("tMs", p.tMs)
                                put("bestLabel", p.bestLabel)
                                put("speedPxPerSec", p.speedPxPerSec)
                            },
                        )
                    }
                },
            )
        }
        s.swipeDecodeDiagnostics?.let { d ->
            put(
                "swipeDecodeDiagnostics",
                JSONObject().apply {
                    put("tuningRevision", d.tuningRevision)
                    put("pathLen", d.pathLen)
                    put("strokeLen", d.strokeLen)
                    put("intentPath", d.intentPath)
                    put("visitOrder", d.visitOrder)
                    put("transitKeyCount", d.transitKeyCount)
                    put("avgSpeedKeyWidthsPerSec", d.avgSpeedKeyWidthsPerSec)
                    put("isSlowStroke", d.isSlowStroke)
                    put("pointerIntegrityOk", d.pointerIntegrityOk)
                },
            )
        }
        s.swipeTargetWord?.let { put("swipeTargetWord", it) }
        s.swipeStyle?.let { put("swipeStyle", it) }
        s.swipeOutcomeCorrect?.let { put("swipeOutcomeCorrect", it) }
        s.swipeGeometry?.let { put("swipeGeometry", geometryToJson(it)) }
        s.correctionChainId?.let { put("correctionChainId", it) }
        s.correctionAttemptIndex?.let { put("correctionAttemptIndex", it) }
        s.correctionFinalWord?.let { put("correctionFinalWord", it) }
    }

    private fun geometryToJson(g: HeatmapSwipeGeometryVector_v1.Vector): JSONObject = JSONObject().apply {
        put("layoutHash", g.layoutHash)
        put("pointCount", g.pointCount)
        put(
            "segments",
            JSONArray().apply {
                g.segments.forEach { seg ->
                    put(
                        JSONObject().apply {
                            put("kind", seg.kind.name)
                            put("startIndex", seg.startIndex)
                            put("endIndex", seg.endIndex)
                            put("dominantLabel", seg.dominantLabel)
                            put("role", seg.role.name)
                            put("durationMs", seg.durationMs)
                            put("angleDeg", seg.angleDeg)
                        },
                    )
                }
            },
        )
        put(
            "cornerPoints",
            JSONArray().apply {
                g.cornerPoints.forEach { p ->
                    put(
                        JSONObject().apply {
                            put("index", p.index)
                            put("x", p.x)
                            put("y", p.y)
                            put("label", p.label)
                            put("role", p.role.name)
                            put("angleDeg", p.angleDeg)
                        },
                    )
                }
            },
        )
    }

    private fun journalEntryToJson(e: ParagraphJournal_v2.Entry): JSONObject = JSONObject().apply {
        put("slotId", e.slotId.value)
        put("word", e.word)
        put("charCount", e.charCount)
        put("wordMemoryOutcome", e.outcome.name)
    }

    private fun liveSummaryToJson(s: HeatmapInstrumentationSnapshot_v6.LiveSummary): JSONObject =
        JSONObject().apply {
            put("sessionGeneration", s.sessionGeneration)
            put("wordsCommitted", s.wordsCommitted)
            put("journalUsedChars", s.journalUsedChars)
            put("journalMaxChars", s.journalMaxChars)
            put("journalWordCount", s.journalWordCount)
            put("lastInputMode", s.lastInputMode)
            put("lastCommitType", s.lastCommitType)
            put("lastTypedWord", s.lastTypedWord)
            put("lastHostPackage", s.lastHostPackage)
        }
}
