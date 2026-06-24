// SPDX-License-Identifier: GPL-3.0-only

// ai-note: v2 — reject memory uses decode snapshot v2 + word memory hooks

package helium314.keyboard.heatmap.swipe

import helium314.keyboard.heatmap.learning.WordSessionInputMode_v1
import helium314.keyboard.heatmap.learning.WordSession_v5
import java.util.Locale

object HeatmapSwipeSlotRejectMemory_v2 {

    private const val MAX_ENTRIES = 80

    data class Entry(
        val pathSignature: String,
        val rejectedWord: String,
        val pathLetters: String,
        val startLabel: String?,
        val endLabel: String?,
        val recordedAtMs: Long,
    )

    private val entries = ArrayDeque<Entry>(MAX_ENTRIES)

    @JvmStatic
    fun recordFromLastSession(session: WordSession_v5?) {
        if (session == null || session.inputMode != WordSessionInputMode_v1.SWIPE) return
        val path = session.swipeIntentPath ?: session.swipeInferredPath ?: return
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
        val snap = HeatmapSwipeDecodeSnapshot_v2.peek() ?: return
        val path = snap.intentPathLetters.ifEmpty { snap.pathLettersDeduped }
        if (path.isEmpty()) return
        recordRejection(
            pathLetters = path,
            startLabel = path.firstOrNull(),
            endLabel = path.lastOrNull(),
            rejectedWord = rejectedWord,
        )
    }

    @JvmStatic
    fun isRejected(
        pathLetters: List<String>,
        startLabel: String?,
        endLabel: String?,
        word: String,
    ): Boolean = HeatmapSwipeSlotRejectMemory_v1.isRejected(
        pathLetters, startLabel, endLabel, word,
    )

    private fun recordRejection(
        pathLetters: List<String>,
        startLabel: String?,
        endLabel: String?,
        rejectedWord: String,
    ) {
        if (rejectedWord.isEmpty() || pathLetters.isEmpty()) return
        val sig = HeatmapSwipeSlotRejectMemory_v1.pathSignature(pathLetters, startLabel, endLabel)
        val key = rejectedWord.lowercase(Locale.US)
        entries.removeAll { it.pathSignature == sig && it.rejectedWord == key }
        entries.addLast(
            Entry(
                pathSignature = sig,
                rejectedWord = key,
                pathLetters = pathLetters.joinToString(""),
                startLabel = startLabel,
                endLabel = endLabel,
                recordedAtMs = System.currentTimeMillis(),
            ),
        )
        while (entries.size > MAX_ENTRIES) entries.removeFirst()
    }
}
