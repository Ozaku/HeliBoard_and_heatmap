// SPDX-License-Identifier: GPL-3.0-only

// ai-note: Block 3 step 15r — classified corner beats + v5 labels; v2 used all raw points + v6 = garbage paths

package helium314.keyboard.heatmap.swipe

import helium314.keyboard.heatmap.learning.HeatmapCoordinateMap_v1
import helium314.keyboard.keyboard.Keyboard
import helium314.keyboard.latin.common.InputPointers

object HeatmapSwipeRawBeatInfer_v3 {

    data class Result(
        val startKeyLabel: String?,
        val pathLetters: List<String>,
        val endKeyLabel: String?,
        val beatCount: Int,
        val beatCountRaw: Int,
        val classifiedBeats: List<HeatmapGeometryClassifier_v2.ClassifiedBeat>,
        val beatIndices: List<Int>,
        val rawBeats: HeatmapSwipeBeat_v3.Result,
    )

    fun infer(keyboard: Keyboard, pointers: InputPointers): Result? {
        val layout = HeatmapCoordinateMap_v1.fromKeyboard(keyboard) ?: return null
        val rawBeats = HeatmapSwipeBeat_v3.detect(pointers, layout)
        if (rawBeats.beatPoints.isEmpty()) return null
        val classified = HeatmapGeometryClassifier_v2.classify(layout, pointers, rawBeats)
        val pathLetters = HeatmapSwipeCornerPathBuilder_v1.fromClassifiedBeats(layout, classified)
        if (pathLetters.isEmpty()) return null
        return Result(
            startKeyLabel = pathLetters.firstOrNull(),
            pathLetters = pathLetters,
            endKeyLabel = pathLetters.lastOrNull(),
            beatCount = classified.size,
            beatCountRaw = rawBeats.beatCount,
            classifiedBeats = classified,
            beatIndices = classified.map { it.index },
            rawBeats = rawBeats,
        )
    }
}
