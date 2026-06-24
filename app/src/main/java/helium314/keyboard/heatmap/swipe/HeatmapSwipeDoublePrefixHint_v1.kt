// SPDX-License-Identifier: GPL-3.0-only

// ai-note: Block 3 step 15s — which path index may get ONE dict double (feet from f,e,t only when warranted)

package helium314.keyboard.heatmap.swipe

object HeatmapSwipeDoublePrefixHint_v1 {

    private const val FEET_BEAT_MIN = 4

    @JvmStatic
    fun hintIndices(
        pathLetters: List<String>,
        beatCountRaw: Int,
        wiggleHints: List<HeatmapSwipeKeyWiggleDetector_v2.Hint>,
    ): Set<Int> {
        val out = LinkedHashSet<Int>()
        for (hint in wiggleHints) {
            if (hint.pathIndex in pathLetters.indices) {
                out.add(hint.pathIndex)
            }
        }
        if (pathLetters.size == 3 && beatCountRaw >= FEET_BEAT_MIN) {
            out.add(1)
        }
        return out.filter { idx ->
            idx in 1 until pathLetters.lastIndex
        }.toSet()
    }
}
