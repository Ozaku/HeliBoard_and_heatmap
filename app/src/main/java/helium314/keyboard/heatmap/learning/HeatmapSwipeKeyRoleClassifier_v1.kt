// SPDX-License-Identifier: GPL-3.0-only

// ai-note: classifies swipe key labels vs final committed word for geometry vectors

package helium314.keyboard.heatmap.learning

import java.util.Locale

object HeatmapSwipeKeyRoleClassifier_v1 {

    @JvmStatic
    fun roleForLabel(finalWord: String, keyLabel: String?): HeatmapSwipeGeometryVector_v1.KeyRole {
        if (keyLabel.isNullOrEmpty()) return HeatmapSwipeGeometryVector_v1.KeyRole.GAP
        val word = finalWord.lowercase(Locale.US)
        if (word.isEmpty()) return HeatmapSwipeGeometryVector_v1.KeyRole.EXTRA
        val label = keyLabel.lowercase(Locale.US)
        val first = word.firstOrNull()?.toString()
        val last = word.lastOrNull()?.toString()
        when (label) {
            first -> return HeatmapSwipeGeometryVector_v1.KeyRole.FIRST
            last -> return if (word.length == 1) {
                HeatmapSwipeGeometryVector_v1.KeyRole.FIRST
            } else {
                HeatmapSwipeGeometryVector_v1.KeyRole.LAST
            }
        }
        if (word.contains(label)) return HeatmapSwipeGeometryVector_v1.KeyRole.IN_WORD
        return HeatmapSwipeGeometryVector_v1.KeyRole.EXTRA
    }

    /** ai-note: middle letters only — excludes first/last anchors */
    @JvmStatic
    fun isMiddleLetter(finalWord: String, keyLabel: String?): Boolean {
        val word = finalWord.lowercase(Locale.US)
        if (word.length <= 2) return false
        val label = keyLabel?.lowercase(Locale.US) ?: return false
        val middle = word.substring(1, word.lastIndex)
        return middle.contains(label)
    }

    @JvmStatic
    fun roleForMiddleLetter(finalWord: String, keyLabel: String?): HeatmapSwipeGeometryVector_v1.KeyRole {
        if (keyLabel.isNullOrEmpty()) return HeatmapSwipeGeometryVector_v1.KeyRole.GAP
        val base = roleForLabel(finalWord, keyLabel)
        if (base == HeatmapSwipeGeometryVector_v1.KeyRole.IN_WORD &&
            isMiddleLetter(finalWord, keyLabel)
        ) {
            return HeatmapSwipeGeometryVector_v1.KeyRole.MIDDLE
        }
        return base
    }
}
