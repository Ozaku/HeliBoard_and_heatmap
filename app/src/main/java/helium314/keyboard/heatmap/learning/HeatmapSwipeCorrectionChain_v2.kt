// SPDX-License-Identifier: GPL-3.0-only

// ai-note: v2 — COHERENCE-GATED correction chains. v1 blindly attached EVERY pending erased swipe
// to the next kept word, producing garbage like "way x6 -> did" (6 abandoned 'way' swipes wrongly
// recorded as failed attempts at 'did'). v2 only groups an erased attempt with a kept word when BOTH:
//   (1) same field position  -> same slotId OR same fieldStartOffset (or offsets unknown = lenient)
//   (2) target-compatible    -> the kept word is a plausible decode of the attempt's RAW swept path:
//        its first letter neighbours the path's first key AND its letters form a neighbour-tolerant
//        in-order subsequence of the attempt's swipeVisitOrder (the geometry-derived key sequence,
//        NOT the decoded attemptText). No full re-decode -> zero per-keystroke cost.
// Incompatible pending attempts are preserved in an ORPHAN bucket (full geometry kept, flagged) so
// the abandoned/mind-change data is not lost, just not mis-attributed.
// AI EDIT MAP:
//   onErasedAttempt()      -> push a pending attempt (carries offset + visitOrder + start key)
//   onKeptWord()           -> partition pending into a CLEAN chain vs ORPHAN bucket for this kept word
//   onKeptTapWord()        -> same, for a tap-typed final word after erased swipes
//   isTargetCompatible()   -> the (1)+(2) gate above (also reused by unit tests)
//   copyResolvedOldestFirst()/copyOrphansOldestFirst() -> export readouts
package helium314.keyboard.heatmap.learning

import helium314.keyboard.heatmap.swipe.HeatmapKeyNeighborGraph_v2
import java.util.ArrayDeque

object HeatmapSwipeCorrectionChain_v2 {

    const val MAX_PENDING = 20
    const val MAX_ORPHANS = 60
    const val MAX_RESOLVED = 60

    /** ai-note: minimum fraction of kept-word letters that must match the path as a subsequence. */
    const val SUBSEQUENCE_MIN_COVERAGE = 0.6

    data class ChainAttempt(
        val memorySequence: Int,
        val attemptText: String,
        val outcome: WordSessionOutcome_v1,
        val geometry: HeatmapSwipeGeometryVector_v1.Vector?,
        val fieldStartOffset: Int = -1,
        val swipeVisitOrder: String? = null,
    )

    data class ResolvedChain(
        val chainId: Int,
        val finalWord: String,
        val finalStartOffset: Int,
        val attempts: List<ChainAttempt>,
    )

    /** ai-note: an erased attempt that did NOT match the next kept word — abandoned / mind-change. */
    data class OrphanAttempt(
        val memorySequence: Int,
        val attemptText: String,
        val outcome: WordSessionOutcome_v1,
        val geometry: HeatmapSwipeGeometryVector_v1.Vector?,
        val fieldStartOffset: Int,
        val swipeVisitOrder: String?,
        val nearestKeptWord: String?,
    )

    private data class PendingAttempt(
        val memorySequence: Int,
        val attemptText: String,
        val outcome: WordSessionOutcome_v1,
        val geometry: HeatmapSwipeGeometryVector_v1.Vector?,
        val fieldStartOffset: Int,
        val slotId: WordSlotId_v1,
        val swipeVisitOrder: String?,
    )

    private val pending = ArrayDeque<PendingAttempt>()
    private val resolved = ArrayDeque<ResolvedChain>()
    private val orphans = ArrayDeque<OrphanAttempt>()
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
                fieldStartOffset = session.fieldStartOffset,
                slotId = session.slotId,
                swipeVisitOrder = session.swipeVisitOrder,
            ),
        )
        while (pending.size > MAX_PENDING) pending.removeFirst()
    }

    /** Resolves pending erased attempts when the user keeps a SWIPE word. */
    @JvmStatic
    fun onKeptWord(session: WordSession_v5): ResolvedChain? {
        if (session.inputMode != WordSessionInputMode_v1.SWIPE) return null
        if (session.wordMemoryOutcome != WordSessionOutcome_v1.KEPT_IN_FIELD) return null
        return resolveInternal(
            keptWord = session.committedText,
            keptMemorySequence = session.memorySequence,
            keptOutcome = session.wordMemoryOutcome,
            keptGeometry = session.swipeGeometry,
            keptStartOffset = session.fieldStartOffset,
            keptSlotId = session.slotId,
        )
    }

    /** Tap-typed final word after erased swipes — geometry null on the tap attempt itself. */
    @JvmStatic
    fun onKeptTapWord(
        finalWord: String,
        memorySequence: Int,
        fieldStartOffset: Int = -1,
        slotId: WordSlotId_v1? = null,
    ): ResolvedChain? = resolveInternal(
        keptWord = finalWord,
        keptMemorySequence = memorySequence,
        keptOutcome = WordSessionOutcome_v1.KEPT_IN_FIELD,
        keptGeometry = null,
        keptStartOffset = fieldStartOffset,
        keptSlotId = slotId,
    )

    private fun resolveInternal(
        keptWord: String,
        keptMemorySequence: Int,
        keptOutcome: WordSessionOutcome_v1,
        keptGeometry: HeatmapSwipeGeometryVector_v1.Vector?,
        keptStartOffset: Int,
        keptSlotId: WordSlotId_v1?,
    ): ResolvedChain? {
        if (pending.isEmpty()) {
            return null
        }
        val compatible = ArrayList<ChainAttempt>(pending.size)
        val drained = ArrayList<PendingAttempt>(pending.size)
        pending.forEach { p ->
            val samePosition = isSamePosition(p, keptStartOffset, keptSlotId)
            val compat = samePosition && isTargetCompatible(keptWord, p.swipeVisitOrder, p.attemptText)
            if (compat) {
                compatible.add(
                    ChainAttempt(
                        memorySequence = p.memorySequence,
                        attemptText = p.attemptText,
                        outcome = p.outcome,
                        geometry = p.geometry,
                        fieldStartOffset = p.fieldStartOffset,
                        swipeVisitOrder = p.swipeVisitOrder,
                    ),
                )
            } else {
                pushOrphan(p, keptWord)
            }
            drained.add(p)
        }
        pending.clear()
        if (compatible.isEmpty()) {
            return null
        }
        compatible.add(
            ChainAttempt(
                memorySequence = keptMemorySequence,
                attemptText = keptWord,
                outcome = keptOutcome,
                geometry = keptGeometry,
                fieldStartOffset = keptStartOffset,
                swipeVisitOrder = null,
            ),
        )
        val chain = ResolvedChain(
            chainId = nextChainId++,
            finalWord = keptWord,
            finalStartOffset = keptStartOffset,
            attempts = compatible,
        )
        resolved.addLast(chain)
        while (resolved.size > MAX_RESOLVED) resolved.removeFirst()
        return chain
    }

    private fun isSamePosition(
        p: PendingAttempt,
        keptStartOffset: Int,
        keptSlotId: WordSlotId_v1?,
    ): Boolean {
        if (keptSlotId != null && p.slotId == keptSlotId) return true
        // ai-note: offsets unknown on either side -> cannot disprove, stay lenient and rely on target gate.
        if (p.fieldStartOffset < 0 || keptStartOffset < 0) return true
        return p.fieldStartOffset == keptStartOffset
    }

    /**
     * (1) first letter of [keptWord] neighbours the first swept key, and
     * (2) >= SUBSEQUENCE_MIN_COVERAGE of kept-word letters appear, in order, as a neighbour-tolerant
     * subsequence of [visitOrder] (raw swept keys). Falls back to text similarity when no visitOrder.
     */
    @JvmStatic
    fun isTargetCompatible(keptWord: String, visitOrder: String?, attemptText: String?): Boolean {
        val kept = keptWord.lowercase().filter { it.isLetter() }
        if (kept.isEmpty()) return true
        val path = visitOrder?.lowercase()?.filter { it.isLetter() }
        if (path.isNullOrEmpty()) {
            // No geometry to compare (e.g. tap attempt) -> use cheap text similarity at same position.
            val other = attemptText?.lowercase()?.filter { it.isLetter() } ?: return true
            if (other.isEmpty()) return true
            return kept.first() == other.first() ||
                HeatmapKeyNeighborGraph_v2.areNeighbors(null, kept.first().toString(), other.first().toString())
        }
        if (!neighborEq(kept.first(), path.first())) return false
        var pi = 0
        var matched = 0
        for (kc in kept) {
            var found = false
            while (pi < path.length) {
                if (neighborEq(kc, path[pi])) {
                    found = true
                    pi++
                    break
                }
                pi++
            }
            if (found) matched++
        }
        return matched.toDouble() / kept.length >= SUBSEQUENCE_MIN_COVERAGE
    }

    private fun neighborEq(a: Char, b: Char): Boolean {
        if (a == b) return true
        return HeatmapKeyNeighborGraph_v2.areNeighbors(null, a.toString(), b.toString())
    }

    private fun pushOrphan(p: PendingAttempt, nearestKeptWord: String) {
        orphans.addLast(
            OrphanAttempt(
                memorySequence = p.memorySequence,
                attemptText = p.attemptText,
                outcome = p.outcome,
                geometry = p.geometry,
                fieldStartOffset = p.fieldStartOffset,
                swipeVisitOrder = p.swipeVisitOrder,
                nearestKeptWord = nearestKeptWord,
            ),
        )
        while (orphans.size > MAX_ORPHANS) orphans.removeFirst()
    }

    @JvmStatic
    fun hasPending(): Boolean = pending.isNotEmpty()

    @JvmStatic
    fun clearPending() {
        pending.clear()
    }

    @JvmStatic
    fun clearAll() {
        pending.clear()
        resolved.clear()
        orphans.clear()
        nextChainId = 1
    }

    @JvmStatic
    fun copyResolvedOldestFirst(): List<ResolvedChain> = resolved.toList()

    @JvmStatic
    fun copyOrphansOldestFirst(): List<OrphanAttempt> = orphans.toList()
}
