// SPDX-License-Identifier: GPL-3.0-only
// ai-note: ring of committed words sized by paragraph window slider (chars, not slots)
package helium314.keyboard.heatmap.learning

/**
 * Keeps recent committed words within a character budget for correction lookback.
 */
class ParagraphJournal_v1(
    initialMaxChars: Int = HeatmapLearningSettings_v1.PARAGRAPH_WINDOW_DEFAULT,
) {
    data class Entry(
        val slotId: WordSlotId_v1,
        val word: String,
        /** word length plus separator (space/punctuation) chars attributed to this slot */
        val charCount: Int,
    )

    private val entries = ArrayDeque<Entry>()
    private var maxChars: Int = initialMaxChars.coerceAtLeast(1)
    private var usedChars: Int = 0

    fun maxChars(): Int = maxChars

    fun usedChars(): Int = usedChars

    fun entryCount(): Int = entries.size

    fun entriesNewestFirst(): List<Entry> = entries.reversed()

    fun entriesOldestFirst(): List<Entry> = entries.toList()

    fun setMaxChars(chars: Int) {
        maxChars = chars.coerceAtLeast(1)
        trimToBudget() // ai-note: no flush on slider change — only on commit-driven trim
    }

    /** @return number of entries removed by char-budget trim (for WINDOW_SLIDE flush) */
    fun appendCommittedWord(slotId: WordSlotId_v1, word: String, separatorCharCount: Int): Int {
        val safeWord = word.ifEmpty { "?" }
        val charCount = safeWord.length + separatorCharCount.coerceAtLeast(0)
        entries.addLast(Entry(slotId, safeWord, charCount))
        usedChars += charCount
        return trimToBudget()
    }

    fun clear() {
        entries.clear()
        usedChars = 0
    }

    private fun trimToBudget(): Int {
        var evicted = 0
        while (usedChars > maxChars && entries.isNotEmpty()) {
            val removed = entries.removeFirst()
            usedChars -= removed.charCount
            evicted += 1
        }
        return evicted
    }
}
