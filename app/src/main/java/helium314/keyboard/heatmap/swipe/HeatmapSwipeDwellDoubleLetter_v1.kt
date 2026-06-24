// SPDX-License-Identifier: GPL-3.0-only

// ai-note: Block 3 step 15i — dwell on E → fee/feet path variants; no triple letters

package helium314.keyboard.heatmap.swipe

object HeatmapSwipeDwellDoubleLetter_v1 {

    @JvmStatic
    fun expandPaths(
        normalized: HeatmapPathLettersNormalize_v2.Normalized,
    ): List<List<String>> {
        val base = normalized.letters
        if (base.isEmpty()) return emptyList()
        val out = LinkedHashSet<List<String>>()
        out.add(base)
        for (hint in normalized.dwellHints) {
            if (hint.rawRunLength < 2) continue
            val idx = hint.dedupedIndex.coerceIn(0, base.lastIndex)
            val letter = hint.letter
            val doubled = base.take(idx) + listOf(letter, letter) + base.drop(idx + 1)
            out.add(doubled)
            if (idx < base.lastIndex) {
                val insertBeforeNext = base.take(idx + 1) + listOf(letter) + base.drop(idx + 1)
                out.add(insertBeforeNext)
            }
        }
        return out.filter { path ->
            !hasTripleRun(path.joinToString(""))
        }.toList()
    }

    @JvmStatic
    fun dwellDoubleChars(normalized: HeatmapPathLettersNormalize_v2.Normalized): Set<Char> =
        normalized.dwellHints
            .filter { it.rawRunLength >= 2 }
            .mapNotNull { it.letter.firstOrNull() }
            .toSet()

    private fun hasTripleRun(joined: String): Boolean {
        if (joined.length < 3) return false
        for (i in 2 until joined.length) {
            if (joined[i] == joined[i - 1] && joined[i] == joined[i - 2]) return true
        }
        return false
    }
}
