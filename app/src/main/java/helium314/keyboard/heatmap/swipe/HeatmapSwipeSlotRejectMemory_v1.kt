// SPDX-License-Identifier: GPL-3.0-only

// ai-note: Block 3 step 15h — remember rejected words per path; track repeated missed letters (learning prep)

package helium314.keyboard.heatmap.swipe

import helium314.keyboard.heatmap.learning.WordSessionInputMode_v1
import helium314.keyboard.heatmap.learning.WordSession_v3
import java.util.Locale

object HeatmapSwipeSlotRejectMemory_v1 {

    private const val MAX_ENTRIES = 40

    data class Entry(
        val pathSignature: String,
        val rejectedWord: String,
        val pathLetters: String,
        val startLabel: String?,
        val endLabel: String?,
        val missedPathIndex: Int?,
        val missedLetter: String?,
        val recordedAtMs: Long,
    )

    private val entries = ArrayDeque<Entry>(MAX_ENTRIES)

    @JvmStatic
    fun pathSignature(pathLetters: List<String>, startLabel: String?, endLabel: String?): String {
        val path = pathLetters.joinToString("")
        return "${startLabel.orEmpty()}|$path|${endLabel.orEmpty()}"
    }

    @JvmStatic
    fun recordRejection(
        pathLetters: List<String>,
        startLabel: String?,
        endLabel: String?,
        rejectedWord: String,
    ) {
        if (rejectedWord.isEmpty() || pathLetters.isEmpty()) return
        val sig = pathSignature(pathLetters, startLabel, endLabel)
        val key = rejectedWord.lowercase(Locale.US)
        entries.removeAll { it.pathSignature == sig && it.rejectedWord == key }
        entries.addLast(
            Entry(
                pathSignature = sig,
                rejectedWord = key,
                pathLetters = pathLetters.joinToString(""),
                startLabel = startLabel,
                endLabel = endLabel,
                missedPathIndex = null,
                missedLetter = null,
                recordedAtMs = System.currentTimeMillis(),
            ),
        )
        trim()
    }

    @JvmStatic
    fun recordMissAtPosition(
        pathLetters: List<String>,
        startLabel: String?,
        endLabel: String?,
        rejectedWord: String,
        pathIndex: Int,
        intendedLetter: String,
    ) {
        recordRejection(pathLetters, startLabel, endLabel, rejectedWord)
        val sig = pathSignature(pathLetters, startLabel, endLabel)
        entries.addLast(
            Entry(
                pathSignature = sig,
                rejectedWord = rejectedWord.lowercase(Locale.US),
                pathLetters = pathLetters.joinToString(""),
                startLabel = startLabel,
                endLabel = endLabel,
                missedPathIndex = pathIndex,
                missedLetter = intendedLetter.lowercase(Locale.US),
                recordedAtMs = System.currentTimeMillis(),
            ),
        )
        trim()
    }

    @JvmStatic
    fun recordFromLastSession(session: WordSession_v3?) {
        if (session == null || session.inputMode != WordSessionInputMode_v1.SWIPE) return
        val path = session.swipeInferredPath
        if (path.isNullOrEmpty()) return
        val letters = path.lowercase(Locale.US).map { it.toString() }
        recordRejection(
            pathLetters = letters,
            startLabel = letters.firstOrNull(),
            endLabel = letters.lastOrNull(),
            rejectedWord = session.committedText,
        )
    }

    @JvmStatic
    fun recordFromDecodeSnapshot(rejectedWord: String) {
        val snap = HeatmapSwipeDecodeSnapshot_v1.peek() ?: return
        if (snap.pathLettersDeduped.isEmpty()) return
        recordRejection(
            pathLetters = snap.pathLettersDeduped,
            startLabel = snap.pathLettersDeduped.firstOrNull(),
            endLabel = snap.pathLettersDeduped.lastOrNull(),
            rejectedWord = rejectedWord,
        )
    }

    @JvmStatic
    fun isRejected(
        pathLetters: List<String>,
        startLabel: String?,
        endLabel: String?,
        word: String,
    ): Boolean {
        val key = word.lowercase(Locale.US)
        val sig = pathSignature(pathLetters, startLabel, endLabel)
        if (entries.any { it.pathSignature == sig && it.rejectedWord == key }) return true
        return entries.any { entry ->
            entry.rejectedWord == key &&
                entry.startLabel == startLabel &&
                entry.endLabel == endLabel &&
                kotlin.math.abs(entry.pathLetters.length - pathLetters.joinToString("").length) <= 1
        }
    }

    @JvmStatic
    fun filterRanked(
        ranked: List<helium314.keyboard.latin.SuggestedWords.SuggestedWordInfo>,
        infer: HeatmapSwipeSegmentInfer_v7.Result?,
    ): List<helium314.keyboard.latin.SuggestedWords.SuggestedWordInfo> {
        if (infer == null || ranked.isEmpty()) return ranked
        val kept = ranked.filter { info ->
            val word = info.mWord?.toString() ?: return@filter false
            !isRejected(infer.pathLetters, infer.startKeyLabel, infer.endKeyLabel, word)
        }
        return if (kept.isNotEmpty()) kept else ranked.drop(1)
    }

    @JvmStatic
    fun clear() {
        entries.clear()
    }

    private fun trim() {
        while (entries.size > MAX_ENTRIES) entries.removeFirst()
    }
}
