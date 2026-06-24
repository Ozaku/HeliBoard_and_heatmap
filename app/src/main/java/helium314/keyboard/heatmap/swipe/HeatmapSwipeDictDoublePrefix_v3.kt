// SPDX-License-Identifier: GPL-3.0-only

// ai-note: Block 3 step 15s — double ONE hinted index only; v1/v2 doubled every letter → tty, fflying

package helium314.keyboard.heatmap.swipe

object HeatmapSwipeDictDoublePrefix_v3 {

    private const val MAX_VARIANTS = 4

    @JvmStatic
    fun prefixVariantsAtIndices(
        pathLetters: List<String>,
        maxLen: Int,
        hintIndices: Set<Int>,
    ): List<String> {
        if (pathLetters.size < 2 || hintIndices.isEmpty()) return emptyList()
        val out = LinkedHashSet<String>()
        for (idx in hintIndices.sorted()) {
            if (idx !in pathLetters.indices) continue
            val doubled = pathLetters.take(idx) +
                listOf(pathLetters[idx], pathLetters[idx]) +
                pathLetters.drop(idx + 1)
            val joined = doubled.joinToString("")
            if (joined.length <= maxLen && !hasTripleRun(joined)) {
                out.add(joined)
            }
        }
        return out.take(MAX_VARIANTS)
    }

    private fun hasTripleRun(joined: String): Boolean {
        if (joined.length < 3) return false
        for (i in 2 until joined.length) {
            if (joined[i] == joined[i - 1] && joined[i] == joined[i - 2]) return true
        }
        return false
    }
}
