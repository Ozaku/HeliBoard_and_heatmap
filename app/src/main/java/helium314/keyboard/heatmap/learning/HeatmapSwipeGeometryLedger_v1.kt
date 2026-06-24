// SPDX-License-Identifier: GPL-3.0-only

// ai-note: ring buffer of resolved swipe correction chains for JSON export

package helium314.keyboard.heatmap.learning

import java.util.ArrayDeque

object HeatmapSwipeGeometryLedger_v1 {

    const val MAX_RESOLVED_CHAINS = 50

    private val resolved = ArrayDeque<HeatmapSwipeCorrectionChain_v1.ResolvedChain>()

    private val lock = Any()

    @JvmStatic
    fun record(chain: HeatmapSwipeCorrectionChain_v1.ResolvedChain) {
        synchronized(lock) {
            resolved.addLast(chain)
            while (resolved.size > MAX_RESOLVED_CHAINS) {
                resolved.removeFirst()
            }
        }
    }

    @JvmStatic
    fun copyResolvedOldestFirst(): List<HeatmapSwipeCorrectionChain_v1.ResolvedChain> =
        synchronized(lock) { resolved.toList() }

    @JvmStatic
    fun clear() = synchronized(lock) { resolved.clear() }
}
