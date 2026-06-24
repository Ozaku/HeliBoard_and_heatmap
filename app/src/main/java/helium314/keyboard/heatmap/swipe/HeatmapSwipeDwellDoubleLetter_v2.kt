// SPDX-License-Identifier: GPL-3.0-only

// ai-note: Block 3 step 15m — dial back dwell/corner doubles; dictionary handles doubles now

package helium314.keyboard.heatmap.swipe

object HeatmapSwipeDwellDoubleLetter_v2 {

    @JvmStatic
    fun expandPaths(
        normalized: HeatmapPathLettersNormalize_v2.Normalized,
    ): List<List<String>> = listOf(normalized.letters)

    @JvmStatic
    fun dwellDoubleChars(
        normalized: HeatmapPathLettersNormalize_v2.Normalized,
    ): Set<Char> = emptySet()
}
