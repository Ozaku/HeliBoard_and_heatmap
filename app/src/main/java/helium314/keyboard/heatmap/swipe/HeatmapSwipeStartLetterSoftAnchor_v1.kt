// SPDX-License-Identifier: GPL-3.0-only

// ai-note: Phase1 step 3.2 — weighted start anchor + neighbor prefix fan-out; anti-teleport hard reject

package helium314.keyboard.heatmap.swipe

object HeatmapSwipeStartLetterSoftAnchor_v1 {

    @JvmStatic
    fun anchorPath(
        pathLetters: List<String>,
        startDistribution: List<HeatmapKeyLikelihood_v6.LabelWeight>,
        touchedLetters: Set<String>,
    ): List<String> {
        val primary = HeatmapKeyLikelihood_v6.primaryStartLabel(startDistribution) ?: return pathLetters
        if (pathLetters.isEmpty()) return listOf(primary)
        if (pathLetters.first().lowercase() == primary) return pathLetters
        if (primary !in touchedLetters && startDistribution.none { it.label in touchedLetters }) {
            return pathLetters
        }
        return listOf(primary) + pathLetters.drop(1)
    }

    @JvmStatic
    fun allowedStartLetters(
        startDistribution: List<HeatmapKeyLikelihood_v6.LabelWeight>,
        graph: HeatmapKeyNeighborGraph_v2.Graph?,
    ): Set<String> {
        if (startDistribution.isEmpty()) return emptySet()
        val primary = startDistribution.maxByOrNull { it.likelihood }?.label ?: return emptySet()
        val g = graph ?: HeatmapKeyNeighborGraph_v2.staticQwerty()
        val out = LinkedHashSet<String>()
        out.add(primary)
        var frontier = listOf(primary)
        repeat(HeatmapSwipeIntentPrototype_v1.startNeighborHopRadius) {
            val next = ArrayList<String>()
            for (node in frontier) {
                for (n in HeatmapKeyNeighborGraph_v2.neighborsOf(g, node)) {
                    if (out.add(n)) next.add(n)
                }
            }
            frontier = next
        }
        for (w in startDistribution) {
            if (w.likelihood >= 0.15) out.add(w.label)
        }
        return out
    }

    @JvmStatic
    fun filterPrefixes(
        prefixes: Collection<String>,
        startDistribution: List<HeatmapKeyLikelihood_v6.LabelWeight>,
        graph: HeatmapKeyNeighborGraph_v2.Graph?,
    ): List<String> {
        val allowed = allowedStartLetters(startDistribution, graph)
        if (allowed.isEmpty()) return prefixes.filter { it.isNotEmpty() }.distinct()
        val primary = HeatmapKeyLikelihood_v6.primaryStartLabel(startDistribution)
        val anchored = prefixes.filter { p ->
            val first = p.firstOrNull()?.lowercaseChar()?.toString() ?: return@filter false
            if (first !in allowed) return@filter false
            if (primary != null && HeatmapSwipeStartLetterAnchor_v1.hopDistance(graph, primary, first) >
                HeatmapSwipeTuningConstants_v1.START_HARD_REJECT_HOPS
            ) {
                return@filter false
            }
            true
        }
        if (anchored.isNotEmpty()) return anchored
        return listOfNotNull(primary?.takeIf { it.isNotEmpty() })
    }

    @JvmStatic
    fun wordAllowedAtStart(
        word: String,
        startDistribution: List<HeatmapKeyLikelihood_v6.LabelWeight>,
        graph: HeatmapKeyNeighborGraph_v2.Graph?,
    ): Boolean {
        val letters = HeatmapSwipeContractionExpand_v1.lettersOnly(word)
        val first = letters.firstOrNull()?.toString() ?: return false
        val allowed = allowedStartLetters(startDistribution, graph)
        if (first !in allowed) return false
        val primary = HeatmapKeyLikelihood_v6.primaryStartLabel(startDistribution) ?: return true
        return HeatmapSwipeStartLetterAnchor_v1.hopDistance(graph, primary, first) <=
            HeatmapSwipeTuningConstants_v1.START_HARD_REJECT_HOPS
    }

    @JvmStatic
    fun prefixVariantsFromStart(
        pathLetters: List<String>,
        startDistribution: List<HeatmapKeyLikelihood_v6.LabelWeight>,
        maxLen: Int,
    ): List<String> {
        val primary = HeatmapKeyLikelihood_v6.primaryStartLabel(startDistribution) ?: return emptyList()
        val out = LinkedHashSet<String>()
        out.add(primary)
        if (pathLetters.isNotEmpty()) {
            val joined = pathLetters.joinToString("")
            if (joined.startsWith(primary)) out.add(joined.take(maxLen))
            if (pathLetters.size >= 2) {
                out.add((primary + pathLetters[1]).take(maxLen))
            }
        }
        for (w in startDistribution) {
            if (w.label != primary && w.likelihood >= 0.2) {
                out.add(w.label)
                if (pathLetters.size >= 2) {
                    out.add((w.label + pathLetters.getOrElse(1) { "" }).take(maxLen))
                }
            }
        }
        return out.filter { it.isNotEmpty() && it.length <= maxLen }.toList()
    }
}
