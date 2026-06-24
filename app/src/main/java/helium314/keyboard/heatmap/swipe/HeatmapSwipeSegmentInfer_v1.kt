// SPDX-License-Identifier: GPL-3.0-only

// ai-note: Block 3 step 13c — map beat points to key labels via coordinate map

package helium314.keyboard.heatmap.swipe

import helium314.keyboard.heatmap.learning.HeatmapCoordinateMap_v1
import helium314.keyboard.keyboard.Keyboard
import helium314.keyboard.latin.common.InputPointers

object HeatmapSwipeSegmentInfer_v1 {

    data class Result(
        val startKeyLabel: String?,
        val pathLetters: List<String>,
        val endKeyLabel: String?,
        val beatCount: Int,
    )

    fun infer(keyboard: Keyboard, pointers: InputPointers): Result? {
        val layout = HeatmapCoordinateMap_v1.fromKeyboard(keyboard) ?: return null
        val beats = HeatmapSwipeBeat_v1.detect(pointers)
        if (beats.beatPoints.isEmpty()) return null
        val labels = beats.beatPoints.map { pt ->
            layout.keyAt(pt.x, pt.y)?.storageLabel
        }
        val pathLetters = labels.filterNotNull()
        return Result(
            startKeyLabel = labels.firstOrNull(),
            pathLetters = pathLetters,
            endKeyLabel = labels.lastOrNull(),
            beatCount = beats.beatCount,
        )
    }
}
