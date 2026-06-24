// SPDX-License-Identifier: GPL-3.0-only
// ai-note: opaque ID for one word position in the paragraph journal (slot does not carry to next word)
package helium314.keyboard.heatmap.learning

/**
 * Identifies one word slot in the current IME learning session.
 * IDs are monotonic within a session and reset when the session resets.
 */
@JvmInline
value class WordSlotId_v1(val value: Int) {
    init {
        require(value > 0) { "WordSlotId must be positive, got $value" }
    }

    override fun toString(): String = "WordSlot#$value"
}
