// SPDX-License-Identifier: GPL-3.0-only

// ai-note: Block 3 step 15b — stash last swipe decode for session export on commit

package helium314.keyboard.heatmap.swipe

object HeatmapSwipeDecodeSnapshot_v1 {

    private const val MAX_RUNNER_UPS = 8

    data class Snapshot(
        val pathLettersRaw: List<String>,
        val pathLettersDeduped: List<String>,
        val runnerUpWords: List<String>,
    )

    private val holder = ThreadLocal<Snapshot?>()

    @JvmStatic
    fun stash(pathRaw: List<String>, pathDeduped: List<String>, rankedWords: List<String>) {
        holder.set(
            Snapshot(
                pathLettersRaw = pathRaw,
                pathLettersDeduped = pathDeduped,
                runnerUpWords = rankedWords.filter { it.isNotEmpty() }.take(MAX_RUNNER_UPS),
            ),
        )
    }

    @JvmStatic
    fun consume(): Snapshot? {
        val snap = holder.get()
        holder.set(null)
        return snap
    }

    /** ai-note: read last decode without consuming (reject memory on backspace) */
    @JvmStatic
    fun peek(): Snapshot? = holder.get()
}
