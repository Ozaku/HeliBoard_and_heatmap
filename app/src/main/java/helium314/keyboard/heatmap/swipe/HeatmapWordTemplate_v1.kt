// SPDX-License-Identifier: GPL-3.0-only

// ai-note: v1 — ideal gesture path (polyline through key centers) for a dictionary word.
// Repeated letters resolve to the same physical key center (handled downstream by the
// arc-length prior + resampling). Returns null if any letter has no key on this layout.
package helium314.keyboard.heatmap.swipe

object HeatmapWordTemplate_v1 {

    fun build(word: String, keyModel: HeatmapGestureKeyModel_v1): List<DoubleArray>? {
        val pts = ArrayList<DoubleArray>(word.length)
        for (ch in word) {
            if (!ch.isLetter()) continue
            val c = keyModel.centerFor(ch) ?: return null
            pts.add(doubleArrayOf(c[0], c[1]))
        }
        return if (pts.isEmpty()) null else pts
    }
}
