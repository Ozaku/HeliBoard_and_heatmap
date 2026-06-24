// SPDX-License-Identifier: GPL-3.0-only

// ai-note: links erased swipe attempts to the next kept word as a correction chain

package helium314.keyboard.heatmap.learning

import java.util.ArrayDeque

object HeatmapSwipeCorrectionChain_v1 {

    const val MAX_PENDING = 20

    data class ChainAttempt(
        val memorySequence: Int,
        val attemptText: String,
        val outcome: WordSessionOutcome_v1,
        val geometry: HeatmapSwipeGeometryVector_v1.Vector?,
    )

    data class ResolvedChain(
        val chainId: Int,
        val finalWord: String,
        val attempts: List<ChainAttempt>,
    )

    private data class PendingAttempt(
        val memorySequence: Int,
        val attemptText: String,
        val outcome: WordSessionOutcome_v1,
        val geometry: HeatmapSwipeGeometryVector_v1.Vector?,
    )

    private val pending = ArrayDeque<PendingAttempt>()
    private var nextChainId = 1

    @JvmStatic
    fun onErasedAttempt(session: WordSession_v5) {
        if (session.inputMode != WordSessionInputMode_v1.SWIPE) return
        if (session.wordMemoryOutcome == WordSessionOutcome_v1.KEPT_IN_FIELD) return
        pending.addLast(
            PendingAttempt(
                memorySequence = session.memorySequence,
                attemptText = session.committedText,
                outcome = session.wordMemoryOutcome,
                geometry = session.swipeGeometry,
            ),
        )
        while (pending.size > MAX_PENDING) {
            pending.removeFirst()
        }
    }

    /** ai-note: resolves pending erased attempts when user keeps a swipe word */
    @JvmStatic
    fun onKeptWord(session: WordSession_v5): ResolvedChain? {
        if (session.inputMode != WordSessionInputMode_v1.SWIPE) return null
        if (session.wordMemoryOutcome != WordSessionOutcome_v1.KEPT_IN_FIELD) return null
        if (pending.isEmpty()) return null
        val attempts = ArrayList<ChainAttempt>(pending.size + 1)
        pending.forEach { p ->
            attempts.add(
                ChainAttempt(
                    memorySequence = p.memorySequence,
                    attemptText = p.attemptText,
                    outcome = p.outcome,
                    geometry = p.geometry,
                ),
            )
        }
        attempts.add(
            ChainAttempt(
                memorySequence = session.memorySequence,
                attemptText = session.committedText,
                outcome = session.wordMemoryOutcome,
                geometry = session.swipeGeometry,
            ),
        )
        pending.clear()
        return ResolvedChain(
            chainId = nextChainId++,
            finalWord = session.committedText,
            attempts = attempts,
        )
    }

    /** ai-note: tap-typed final word after erased swipes — geometry null on tap attempt */
    @JvmStatic
    fun onKeptTapWord(finalWord: String, memorySequence: Int): ResolvedChain? {
        if (pending.isEmpty()) return null
        val attempts = ArrayList<ChainAttempt>(pending.size + 1)
        pending.forEach { p ->
            attempts.add(
                ChainAttempt(
                    memorySequence = p.memorySequence,
                    attemptText = p.attemptText,
                    outcome = p.outcome,
                    geometry = p.geometry,
                ),
            )
        }
        attempts.add(
            ChainAttempt(
                memorySequence = memorySequence,
                attemptText = finalWord,
                outcome = WordSessionOutcome_v1.KEPT_IN_FIELD,
                geometry = null,
            ),
        )
        pending.clear()
        return ResolvedChain(
            chainId = nextChainId++,
            finalWord = finalWord,
            attempts = attempts,
        )
    }

    @JvmStatic
    fun hasPending(): Boolean = pending.isNotEmpty()

    @JvmStatic
    fun clearPending() {
        pending.clear()
    }

    @JvmStatic
    fun copyResolvedOldestFirst(): List<ResolvedChain> =
        HeatmapSwipeGeometryLedger_v1.copyResolvedOldestFirst()
}
