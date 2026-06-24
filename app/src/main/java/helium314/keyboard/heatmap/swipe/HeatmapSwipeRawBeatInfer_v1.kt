// SPDX-License-Identifier: GPL-3.0-only

// ai-note: Block 3 step 15o — beat v2 + classifier v2 raw path (may include f,e,e,t)

package helium314.keyboard.heatmap.swipe

import helium314.keyboard.heatmap.learning.HeatmapCoordinateMap_v1
import helium314.keyboard.keyboard.Keyboard
import helium314.keyboard.latin.common.InputPointers

object HeatmapSwipeRawBeatInfer_v1 {

    data class Result(
        val startKeyLabel: String?,
        val pathLetters: List<String>,
        val endKeyLabel: String?,
        val beatCount: Int,
        val beatCountRaw: Int,
        val classifiedBeats: List<HeatmapGeometryClassifier_v2.ClassifiedBeat>,
        val beatIndices: List<Int>,
    )

    fun infer(keyboard: Keyboard, pointers: InputPointers): Result? {
        val layout = HeatmapCoordinateMap_v1.fromKeyboard(keyboard) ?: return null
        val rawBeats = HeatmapSwipeBeat_v2.detect(pointers, layout)
        if (rawBeats.beatPoints.isEmpty()) return null
        val classified = HeatmapGeometryClassifier_v2.classify(layout, pointers, rawBeats)
        val labels = classified.mapNotNull { beat ->
            beat.keyLabel ?: HeatmapKeyLikelihood_v5.bestLabelAt(layout, beat.x, beat.y)
        }
        if (labels.isEmpty()) return null
        return Result(
            startKeyLabel = labels.firstOrNull(),
            pathLetters = labels,
            endKeyLabel = labels.lastOrNull(),
            beatCount = classified.size,
            beatCountRaw = rawBeats.beatCount,
            classifiedBeats = classified,
            beatIndices = classified.map { it.index },
        )
    }
}
