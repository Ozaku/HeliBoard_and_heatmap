// SPDX-License-Identifier: GPL-3.0-only

// ai-note: Block 3 step 15g — layout-derived key neighbors + QWERTY fallback for near-miss scoring

package helium314.keyboard.heatmap.swipe

import helium314.keyboard.heatmap.learning.HeatmapCoordinateMap_v1
import kotlin.math.hypot
import kotlin.math.min

object HeatmapKeyNeighborGraph_v1 {

    private const val NEIGHBOR_DISTANCE_FACTOR = 1.28

    private val QWERTY_ROW1 = "qwertyuiop"
    private val QWERTY_ROW2 = "asdfghjkl"
    private val QWERTY_ROW3 = "zxcvbnm"

    data class Graph(val byLetter: Map<String, Set<String>>)

    @JvmStatic
    fun fromLayout(snapshot: HeatmapCoordinateMap_v1.Snapshot): Graph {
        val letterKeys = snapshot.keys.filter { it.storageLabel.length == 1 && it.storageLabel[0].isLetter() }
        val neighbors = HashMap<String, MutableSet<String>>()
        for (key in letterKeys) {
            val label = key.storageLabel
            val reach = min(key.right - key.left, key.bottom - key.top).coerceAtLeast(1) * NEIGHBOR_DISTANCE_FACTOR
            val set = neighbors.getOrPut(label) { LinkedHashSet() }
            for (other in letterKeys) {
                if (other.storageLabel == label) continue
                val dx = (key.centerX - other.centerX).toDouble()
                val dy = (key.centerY - other.centerY).toDouble()
                if (hypot(dx, dy) <= reach) set.add(other.storageLabel)
            }
            mergeStaticQwerty(label, set)
        }
        return Graph(neighbors)
    }

    @JvmStatic
    fun staticQwerty(): Graph {
        val neighbors = HashMap<String, MutableSet<String>>()
        addRowNeighbors(neighbors, QWERTY_ROW1)
        addRowNeighbors(neighbors, QWERTY_ROW2)
        addRowNeighbors(neighbors, QWERTY_ROW3)
        bridgeRows(neighbors, QWERTY_ROW1, QWERTY_ROW2)
        bridgeRows(neighbors, QWERTY_ROW2, QWERTY_ROW3)
        return Graph(neighbors)
    }

    @JvmStatic
    fun neighborsOf(graph: Graph?, letter: String): Set<String> {
        val lower = letter.lowercase()
        return graph?.byLetter?.get(lower).orEmpty().ifEmpty { staticQwerty().byLetter[lower].orEmpty() }
    }

    @JvmStatic
    fun areNeighbors(graph: Graph?, a: String, b: String): Boolean {
        val la = a.lowercase()
        val lb = b.lowercase()
        if (la == lb) return true
        return neighborsOf(graph, la).contains(lb) || neighborsOf(graph, lb).contains(la)
    }

    private fun mergeStaticQwerty(label: String, set: MutableSet<String>) {
        for (n in staticQwerty().byLetter[label].orEmpty()) set.add(n)
    }

    private fun addRowNeighbors(map: MutableMap<String, MutableSet<String>>, row: String) {
        for (i in row.indices) {
            val label = row[i].toString()
            val set = map.getOrPut(label) { LinkedHashSet() }
            if (i > 0) set.add(row[i - 1].toString())
            if (i < row.lastIndex) set.add(row[i + 1].toString())
        }
    }

    private fun bridgeRows(
        map: MutableMap<String, MutableSet<String>>,
        upper: String,
        lower: String,
    ) {
        for (i in upper.indices) {
            val u = upper[i].toString()
            val candidates = listOfNotNull(
                lower.getOrNull(i - 1)?.toString(),
                lower.getOrNull(i)?.toString(),
                lower.getOrNull(i + 1)?.toString(),
            )
            val set = map.getOrPut(u) { LinkedHashSet() }
            candidates.forEach { set.add(it) }
            for (l in candidates) {
                map.getOrPut(l) { LinkedHashSet() }.add(u)
            }
        }
    }
}
