// SPDX-License-Identifier: GPL-3.0-only

// ai-note: v1 — stroke-order monotonic subsequence checks for prefix + scoring

package helium314.keyboard.heatmap.swipe

object HeatmapSwipeStrokeMonotonicPath_v1 {

    @JvmStatic
    fun isMonotonicSubsequence(candidate: String, orderedPath: List<String>): Boolean {
        if (candidate.isEmpty() || orderedPath.isEmpty()) return false
        val lower = HeatmapSwipeContractionExpand_v1.lettersOnly(candidate)
        if (lower.isEmpty()) return false
        var pathIdx = 0
        for (i in lower.indices) {
            if (i > 0 && lower[i] == lower[i - 1]) continue
            val ch = lower[i].toString()
            var found = false
            for (j in pathIdx until orderedPath.size) {
                if (orderedPath[j].equals(ch, ignoreCase = true)) {
                    pathIdx = j + 1
                    found = true
                    break
                }
            }
            if (!found) return false
        }
        return true
    }

    @JvmStatic
    fun filterPrefixes(prefixes: Collection<String>, orderedPath: List<String>): List<String> {
        if (orderedPath.isEmpty()) return emptyList()
        val pathStr = orderedPath.joinToString("")
        return prefixes
            .filter { prefix ->
                prefix.isNotEmpty() &&
                    prefix.length <= pathStr.length &&
                    pathStr.startsWith(prefix) &&
                    isMonotonicSubsequence(prefix, orderedPath)
            }
            .distinct()
    }

    @JvmStatic
    fun progressivePrefixes(orderedPath: List<String>, maxLen: Int): List<String> {
        if (orderedPath.isEmpty()) return emptyList()
        val join = orderedPath.joinToString("")
        if (join.isEmpty()) return emptyList()
        val cap = maxLen.coerceAtMost(join.length).coerceAtMost(24)
        val out = LinkedHashSet<String>()
        for (len in cap downTo 1) {
            out.add(join.take(len))
        }
        return out.toList().sortedByDescending { it.length }
    }

    /** ai-note: keep consecutive doubles (feet) but never reorder letters */
    @JvmStatic
    fun filterOrderedPath(path: List<String>): List<String> {
        if (path.isEmpty()) return path
        return HeatmapPathLettersNormalize_v1.collapseConsecutiveDuplicates(path)
    }
}
