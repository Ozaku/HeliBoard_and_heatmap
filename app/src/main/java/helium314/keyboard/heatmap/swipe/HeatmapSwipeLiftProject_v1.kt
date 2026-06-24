// SPDX-License-Identifier: GPL-3.0-only

// ai-note: Block 3 step 15h — project past stroke end along tail bearing (corners → intended next key)

package helium314.keyboard.heatmap.swipe

import helium314.keyboard.heatmap.learning.HeatmapCoordinateMap_v1
import helium314.keyboard.latin.common.InputPointers
import kotlin.math.hypot

object HeatmapSwipeLiftProject_v1 {

    private const val TAIL_FRACTION = 0.24
    private const val PROJECT_KEY_WIDTHS = 0.45
    private const val WEAK_LIFT_ON_KEY = 0.52

    @JvmStatic
    fun liftLabel(layout: HeatmapCoordinateMap_v1.Snapshot, pointers: InputPointers): String? {
        val size = pointers.pointerSize
        if (size < 1) return null
        val xs = pointers.xCoordinates
        val ys = pointers.yCoordinates
        val liftX = xs[size - 1]
        val liftY = ys[size - 1]
        val direct = HeatmapKeyLikelihood_v3.bestLabelAt(layout, liftX, liftY) ?: return null
        if (size < 4) return direct
        val tailFrom = (size * (1.0 - TAIL_FRACTION)).toInt().coerceIn(0, size - 2)
        val dx = (liftX - xs[tailFrom]).toDouble()
        val dy = (liftY - ys[tailFrom]).toDouble()
        val span = hypot(dx, dy)
        if (span < 10.0) return direct
        val keyWidth = layout.keys.firstOrNull()?.let { (it.right - it.left).coerceAtLeast(1) } ?: 48
        val projX = (liftX + dx / span * keyWidth * PROJECT_KEY_WIDTHS).toInt()
        val projY = (liftY + dy / span * keyWidth * PROJECT_KEY_WIDTHS).toInt()
        val projected = HeatmapKeyLikelihood_v3.bestLabelAt(layout, projX, projY) ?: return direct
        val directKey = layout.keys.firstOrNull { it.storageLabel == direct }
        val directLike = if (directKey != null) {
            HeatmapKeyLikelihood_v3.likelihoodAt(layout, liftX, liftY, directKey)
        } else {
            1.0
        }
        return if (directLike < WEAK_LIFT_ON_KEY && projected != direct) projected else direct
    }
}
