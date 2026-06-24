// SPDX-License-Identifier: GPL-3.0-only

// ai-note: Phase1 step 1.3 v2 - swipe trace ring for WordSession_v5 export

package helium314.keyboard.heatmap.learning

import helium314.keyboard.heatmap.swipe.HeatmapSwipeTuningConstants_v1
import java.util.ArrayDeque

object HeatmapSwipeTraceRingBuffer_v2 {

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
            tuningRevision = HeatmapSwipeTuningConstants_v1.TUNING_REVISION,
            committedAtMs = session.committedAtMs,
        )
        synchronized(lock) {
            ring.addLast(entry)
            while (ring.size > HeatmapSwipeTuningConstants_v1.SWIPE_RING_BUFFER_SIZE) {
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