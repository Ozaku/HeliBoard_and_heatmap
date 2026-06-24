// SPDX-License-Identifier: GPL-3.0-only

// ai-note: v2 word memory ledger - stores WordSession_v5 entries

package helium314.keyboard.heatmap.learning

object HeatmapWordMemoryLedger_v2 {

    const val MAX_WORD_ENTRIES = 300

    const val MAX_CHAR_BUDGET = 3000

    private val entries = ArrayDeque<WordSession_v5>()

    private var usedChars = 0

    private var nextSequence = 1

    @JvmStatic
    fun record(session: WordSession_v5) {
        entries.addLast(session)
        usedChars += session.memoryCharWeight()
        trimToBudget()
    }

    @JvmStatic
    fun nextMemorySequence(): Int = nextSequence++

    @JvmStatic
    fun entryCount(): Int = entries.size

    @JvmStatic
    fun usedCharBudget(): Int = usedChars

    @JvmStatic
    fun maxCharBudget(): Int = MAX_CHAR_BUDGET

    @JvmStatic
    fun maxWordEntries(): Int = MAX_WORD_ENTRIES

    @JvmStatic
    fun copyOldestFirst(): List<WordSession_v5> = entries.toList()

    @JvmStatic
    fun copyNewestFirst(): List<WordSession_v5> = entries.reversed()

    @JvmStatic
    fun keptCount(): Int =
        entries.count { it.wordMemoryOutcome == WordSessionOutcome_v1.KEPT_IN_FIELD }

    @JvmStatic
    fun erasedCount(): Int =
        entries.count {
            it.wordMemoryOutcome != WordSessionOutcome_v1.KEPT_IN_FIELD
        }

    @JvmStatic
    fun clear() {
        entries.clear()
        usedChars = 0
        nextSequence = 1
    }

    private fun trimToBudget() {
        while (
            (entries.size > MAX_WORD_ENTRIES || usedChars > MAX_CHAR_BUDGET) &&
            entries.isNotEmpty()
        ) {
            val removed = entries.removeFirst()
            usedChars -= removed.memoryCharWeight()
        }
    }
}