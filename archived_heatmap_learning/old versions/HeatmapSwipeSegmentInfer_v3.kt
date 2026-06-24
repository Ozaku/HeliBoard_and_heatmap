// SPDX-License-Identifier: GPL-3.0-only

// ai-note: Block 3 step 15b — v2 inference + deduped pathLetters for decode/scoring

package helium314.keyboard.heatmap.swipe

import helium314.keyboard.keyboard.Keyboard
import helium314.keyboard.latin.common.InputPointers

object HeatmapSwipeSegmentInfer_v3 {

    data class Result(
        val startKeyLabel: String?,
        val pathLetters: List<String>,
        val pathLettersRaw: List<String>,
        val endKeyLabel: String?,
        val beatCount: Int,
        val beatCountRaw: Int,
        val classifiedBeats: List<HeatmapGeometryClassifier_v1.ClassifiedBeat>,
    )

    fun infer(keyboard: Keyboard, pointers: InputPointers): Result? {
        val raw = HeatmapSwipeSegmentInfer_v2.infer(keyboard, pointers) ?: return null
        val deduped = HeatmapPathLettersNormalize_v1.collapseConsecutiveDuplicates(raw.pathLetters)
        if (deduped.isEmpty()) return null
        return Result(
            startKeyLabel = deduped.firstOrNull(),
            pathLetters = deduped,
            pathLettersRaw = raw.pathLetters,
            endKeyLabel = deduped.lastOrNull(),
            beatCount = deduped.size,
            beatCountRaw = raw.beatCount,
            classifiedBeats = raw.classifiedBeats,
        )
    }
}
