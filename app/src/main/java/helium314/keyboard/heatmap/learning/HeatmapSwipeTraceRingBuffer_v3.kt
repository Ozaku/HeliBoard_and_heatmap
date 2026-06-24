// SPDX-License-Identifier: GPL-3.0-only

// ai-note: v3 — swipe trace ring with intent path + diagnostics for export

package helium314.keyboard.heatmap.learning

import helium314.keyboard.heatmap.swipe.HeatmapSwipeTuningConstants_v4
import java.util.ArrayDeque

object HeatmapSwipeTraceRingBuffer_v3 {

    data class Entry(
        val committedText: String,
        val swipeIntentPath: String?,
        val swipeVisitOrder: String?,
        val swipeTargetWord: String?,
        val swipeStyle: String?,
        val wordMemoryOutcome: String?,
        val tracePointCount: Int,
        val geometrySegmentCount: Int,
        val tuningRevision: Int,
        val committedAtMs: Long,
    )

    private val ring = ArrayDeque<Entry>()
    private val lock = Any()

    @JvmStatic
    fun record(session: WordSession_v5) {
        if (session.inputMode != WordSessionInputMode_v1.SWIPE) return
        val entry = Entry(
            committedText = session.committedText,
            swipeIntentPath = session.swipeIntentPath,
            swipeVisitOrder = session.swipeVisitOrder,
            swipeTargetWord = session.swipeTargetWord,
            swipeStyle = session.swipeStyle,
            wordMemoryOutcome = session.wordMemoryOutcome.name,
            tracePointCount = session.swipeTracePointCount,
            geometrySegmentCount = session.swipeGeometry?.segments?.size ?: 0,
            tuningRevision = HeatmapSwipeTuningConstants_v4.TUNING_REVISION,
            committedAtMs = session.committedAtMs,
        )
        synchronized(lock) {
            ring.addLast(entry)
            while (ring.size > HeatmapSwipeTuningConstants_v4.SWIPE_RING_BUFFER_SIZE) {
                ring.removeFirst()
            }
        }
    }

    @JvmStatic
    fun copyEntriesNewestFirst(): List<Entry> = synchronized(lock) {
        ring.reversed().toList()
    }

    @JvmStatic
    fun clear() = synchronized(lock) { ring.clear() }
}
