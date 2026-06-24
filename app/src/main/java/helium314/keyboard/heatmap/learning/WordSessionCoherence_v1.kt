// SPDX-License-Identifier: GPL-3.0-only

// ai-note: v1 — how trustworthy a word session's position/attribution is, for offline filtering.
//   CLEAN   -> offsets known, attempts coherently grouped; safe to train on.
//   ORPHAN  -> an erased attempt that did NOT match the kept word at its position (abandoned /
//              mind-change); preserved but should be excluded from stroke-accuracy training.
//   SUSPECT -> position could not be trusted (unknown cursor, cross-process gap, ambiguous edit);
//              keep the data but flag it so the harness can down-weight or drop it.
package helium314.keyboard.heatmap.learning

enum class WordSessionCoherence_v1 {
    CLEAN,
    ORPHAN,
    SUSPECT,
}
