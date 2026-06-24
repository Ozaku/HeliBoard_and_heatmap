// SPDX-License-Identifier: GPL-3.0-only

// ai-note: Block 3 step 13a — min axis span vs start-key size; blocks tap→false swipe

package helium314.keyboard.heatmap.swipe

import helium314.keyboard.keyboard.Key
import kotlin.math.abs

object HeatmapSwipeTapGate_v1 {

    /**
     * Stroke qualifies as swipe only if horizontal span >= start-key width
     * OR vertical span >= start-key height (44_ / maintainer rule).
     */
    @JvmStatic
    fun qualifiesAsSwipe(
        startX: Int,
        startY: Int,
        currentX: Int,
        currentY: Int,
        startKey: Key?,
        fallbackKeyWidth: Int,
        fallbackKeyHeight: Int,
    ): Boolean {
        val keyWidth = startKey?.hitBox?.let { it.right - it.left }?.coerceAtLeast(1) ?: fallbackKeyWidth.coerceAtLeast(1)
        val keyHeight = startKey?.hitBox?.let { it.bottom - it.top }?.coerceAtLeast(1) ?: fallbackKeyHeight.coerceAtLeast(1)
        val horizSpan = abs(currentX - startX)
        val vertSpan = abs(currentY - startY)
        return horizSpan >= keyWidth || vertSpan >= keyHeight
    }
}
