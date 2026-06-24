// SPDX-License-Identifier: GPL-3.0-only

// ai-note: Block 3 step 15r — no doubles on 2-letter ty→tty accidents; only wiggle/3+ letter paths

package helium314.keyboard.heatmap.swipe

object HeatmapSwipeDictDoublePrefix_v2 {

    private const val MAX_VARIANTS = 8
    private const val MIN_PATH_FOR_AUTO_DOUBLE = 3

    @JvmStatic
    fun prefixVariants(
        pathLetters: List<String>,
        maxLen: Int,
        hasWiggleDouble: Boolean,
    ): List<String> {
        if (pathLetters.size < 2) return emptyList()
        if (pathLetters.size < MIN_PATH_FOR_AUTO_DOUBLE && !hasWiggleDouble) return emptyList()
        val out = LinkedHashSet<String>()
        for (idx in pathLetters.indices) {
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
