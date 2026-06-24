// SPDX-License-Identifier: GPL-3.0-only

// ai-note: v2 — REAL finalText reconciliation (v1 was a fingerprint-only stub that never assigned
// WordSession.finalText). Given the live field model (tracked words + offsets) and a FieldTextProbe_v2
// window, reads the word now occupying each tracked offset and writes it back to that slot's session,
// so "swipe attempt vs the word actually kept in the field" is unambiguous. Also flags proofread
// edits: a cursor jump far from the last commit that lands on an earlier word.
// AI EDIT MAP:
//   reconcileFinalText() -> for each alive FieldWord: probe word at its offset; update registry finalText
//   classifyCursorJump() -> distance-based proofread detection used by HeatmapWordSlotSession_v7
package helium314.keyboard.heatmap.learning

import helium314.keyboard.latin.utils.Log

object InWindowEditDetector_v2 {

    private const val TAG = "HeatmapInstr"

    /** ai-note: a cursor move farther than this many chars from the last commit = proofreading jump. */
    const val PROOFREAD_JUMP_MIN_CHARS = 12

    data class ReconcileResult(val checked: Int, val updated: Int)

    /**
     * Walks every alive tracked word and, if the probe can read the word now at that offset and it
     * differs from the recorded finalText, updates the session for that slot and the field word.
     */
    @JvmStatic
    fun reconcileFinalText(
        fieldModel: HeatmapFieldWordModel_v1,
        probe: FieldTextProbe_v2.WindowProbe?,
        registry: WordSessionRegistry_v5,
    ): ReconcileResult {
        if (probe == null) return ReconcileResult(0, 0)
        var checked = 0
        var updated = 0
        for (fw in fieldModel.snapshotOldestFirst()) {
            if (!fw.alive) continue
            val now = probe.wordCoveringOffset(fw.startOffset) ?: continue
            checked++
            if (!now.equals(fw.word, ignoreCase = false)) {
                if (registry.updateFinalText(fw.slotId, now)) {
                    updated++
                    Log.i(TAG, "finalText reconcile slot=${fw.slotId.value} '${fw.word}' -> '$now'")
                }
            }
        }
        return ReconcileResult(checked, updated)
    }

    /** True when [newStart] jumps far from [lastCommitOffset] (a proofreading move). */
    @JvmStatic
    fun classifyCursorJump(lastCommitOffset: Int, newStart: Int): Boolean {
        if (lastCommitOffset < 0 || newStart < 0) return false
        return kotlin.math.abs(newStart - lastCommitOffset) >= PROOFREAD_JUMP_MIN_CHARS
    }
}
