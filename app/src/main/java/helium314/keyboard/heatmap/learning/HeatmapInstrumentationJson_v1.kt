// SPDX-License-Identifier: GPL-3.0-only

// ai-note: step 1.13 — JSON export for WordSessions + journal + prefs instrumentation

package helium314.keyboard.heatmap.learning



import android.content.Context

import android.content.SharedPreferences

import org.json.JSONArray

import org.json.JSONObject



object HeatmapInstrumentationJson_v1 {

    const val SCHEMA_VERSION = 1



    fun build(

        context: Context,

        sessions: List<WordSession_v2>,

        journalEntries: List<ParagraphJournal_v1.Entry>,

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

        root.put("instrumentation", instrumentationBlock(prefs))
        root.put("metrics", metricsBlock(prefs))
        root.put("learningStore", learningStoreBlock(context))

        root.put("wordSessions", JSONArray().apply { sessions.forEach { put(sessionToJson(it)) } })

        root.put("paragraphJournal", JSONArray().apply { journalEntries.forEach { put(journalEntryToJson(it)) } })

        liveSummary?.let { root.put("liveImeSession", liveSummaryToJson(it)) }

        return root.toString(2)

    }



    /** Settings process: prefs + on-disk snapshot meta when IME memory is unavailable. */

    fun buildPrefsSnapshotOnly(context: Context): String =

        build(

            context,

            sessions = emptyList(),

            journalEntries = emptyList(),

            liveSummary = null,

            source = "settings_prefs_only",

        )



    private fun buildInfo(): JSONObject = JSONObject().apply {

        put("beta", HeatmapLearningBuildInfo_v1.BETA_VERSION)

        put("roadmapStep", HeatmapLearningBuildInfo_v1.ROADMAP_STEP)

        put("statusLine", HeatmapLearningBuildInfo_v1.statusLine())

    }



    private fun settingsBlock(context: Context): JSONObject = JSONObject().apply {

        put("learningEnabled", HeatmapLearningSettings_v2.isLearningEnabled(context))

        put("paragraphWindowChars", HeatmapLearningSettings_v2.getParagraphWindowChars(context))

        put("tapHeatmapAutocorrect", HeatmapAutocorrectSettings_v1.isTapHeatmapAutocorrectEnabled(context))

        put("swipeHeatmapAutocorrect", HeatmapAutocorrectSettings_v1.isSwipeHeatmapAutocorrectEnabled(context))

        put("autoFileDump", HeatmapExportSettings_v1.isAutoFileDumpEnabled(context))

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



    private fun sessionToJson(s: WordSession_v2): JSONObject = JSONObject().apply {

        put("slotId", s.slotId.value)

        put("sessionGeneration", s.sessionGeneration)

        put("hostPackage", s.hostPackage)

        put("inputMode", s.inputMode.name)

        put("commitType", s.commitType.name)

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

    }



    private fun journalEntryToJson(e: ParagraphJournal_v1.Entry): JSONObject = JSONObject().apply {

        put("slotId", e.slotId.value)

        put("word", e.word)

        put("charCount", e.charCount)

    }



    private fun liveSummaryToJson(s: HeatmapInstrumentationSnapshot_v6.LiveSummary): JSONObject =

        JSONObject().apply {

            put("sessionGeneration", s.sessionGeneration)

            put("wordsCommitted", s.wordsCommitted)

            put("journalUsedChars", s.journalUsedChars)

            put("journalMaxChars", s.journalMaxChars)

            put("journalWordCount", s.journalWordCount)

            put("lastSlotId", s.lastSlotId)

            put("lastWord", s.lastWord)

            put("lastHostPackage", s.lastHostPackage)

        }

}

