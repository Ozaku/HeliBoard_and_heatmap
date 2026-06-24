// SPDX-License-Identifier: GPL-3.0-only

// ai-note: Block 3 step 15e — apostrophe-less swipe paths → I'm, isn't, don't, etc.

package helium314.keyboard.heatmap.swipe

object HeatmapSwipeContractionExpand_v1 {

    private val TWO_LETTER = mapOf(
        "im" to "I'm",
        "id" to "I'd",
        "iv" to "I've",
    )

    /** ai-note: swipe nt-ending paths without apostrophe key (isnt → isn't) */
    fun expansions(pathJoin: String): List<String> {
        if (pathJoin.isEmpty()) return emptyList()
        val lower = pathJoin.lowercase()
        val out = LinkedHashSet<String>()
        TWO_LETTER[lower]?.let { out.add(it) }
        if (lower.length >= 4 && lower.endsWith("nt")) {
            out.add(lower.dropLast(2) + "n't")
        }
        if (lower.length >= 3 && lower.endsWith("nt")) {
            val shortForm = lower.dropLast(2) + "n't"
            if (shortForm.length <= 6) out.add(shortForm)
        }
        return out.toList()
    }

    /** Letters-only form for geometry compare (I'm → im). */
    @JvmStatic
    fun lettersOnly(word: String): String =
        word.lowercase().filter { it.isLetter() }.toString()
}
