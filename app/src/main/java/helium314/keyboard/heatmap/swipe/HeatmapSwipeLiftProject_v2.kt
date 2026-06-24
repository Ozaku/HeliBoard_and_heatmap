// SPDX-License-Identifier: GPL-3.0-only

// ai-note: Block 3 step 15k — lift label only from lift point; no projection to untouched keys

package helium314.keyboard.heatmap.swipe

import helium314.keyboard.heatmap.learning.HeatmapCoordinateMap_v1
import helium314.keyboard.latin.common.InputPointers

object HeatmapSwipeLiftProject_v2 {

    @JvmStatic
    fun liftLabel(layout: HeatmapCoordinateMap_v1.Snapshot, pointers: InputPointers): String? {
        val size = pointers.pointerSize
        if (size < 1) return null
        val liftX = pointers.xCoordinates[size - 1]
        val liftY = pointers.yCoordinates[size - 1]
        return HeatmapKeyLikelihood_v5.bestLabelAt(layout, liftX, liftY)
    }
}
