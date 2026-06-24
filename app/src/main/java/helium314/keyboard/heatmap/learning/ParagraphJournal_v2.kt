// SPDX-License-Identifier: GPL-3.0-only

// ai-note: v2 — char budget + max 300 word entries (matches word memory ledger)

package helium314.keyboard.heatmap.learning

class ParagraphJournal_v2(
    initialMaxChars: Int = HeatmapWordMemoryLedger_v1.MAX_CHAR_BUDGET,
    initialMaxWords: Int = HeatmapWordMemoryLedger_v1.MAX_WORD_ENTRIES,
) {
    data class Entry(
        val slotId: WordSlotId_v1,
        val word: String,
        val charCount: Int,
        val outcome: WordSessionOutcome_v1 = WordSessionOutcome_v1.KEPT_IN_FIELD,
    )

    private val entries = ArrayDeque<Entry>()
    private var maxChars = initialMaxChars.coerceAtLeast(1)
    private var maxWords = initialMaxWords.coerceAtLeast(1)
    private var usedChars = 0

    fun maxChars(): Int = maxChars

    fun maxWords(): Int = maxWords

    fun usedChars(): Int = usedChars

    fun entryCount(): Int = entries.size

    fun entriesOldestFirst(): List<Entry> = entries.toList()

    fun setMaxChars(chars: Int) {
        maxChars = chars.coerceIn(
            HeatmapLearningSettings_v1.PARAGRAPH_WINDOW_MIN,
            HeatmapLearningSettings_v1.PARAGRAPH_WINDOW_MAX,
        )
        trimToBudget()
    }

    fun setMaxWords(words: Int) {
        maxWords = words.coerceAtMost(HeatmapWordMemoryLedger_v1.MAX_WORD_ENTRIES)
        trimToBudget()
    }

    /** @return entries evicted by trim */
    fun appendWordAttempt(
        slotId: WordSlotId_v1,
        word: String,
        separatorCharCount: Int,
        outcome: WordSessionOutcome_v1,
    ): Int {
        val safeWord = word.ifEmpty { "?" }
        val charCount = safeWord.length + separatorCharCount.coerceAtLeast(0)
        entries.addLast(Entry(slotId, safeWord, charCount, outcome))
        usedChars += charCount + 1
        return trimToBudget()
    }

    fun clear() {
        entries.clear()
        usedChars = 0
    }

    private fun trimToBudget(): Int {
        var evicted = 0
        while (
            (usedChars > maxChars || entries.size > maxWords) &&
            entries.isNotEmpty()
        ) {
            val removed = entries.removeFirst()
            usedChars -= removed.charCount + 1
            evicted += 1
        }
        return evicted
    }
}
