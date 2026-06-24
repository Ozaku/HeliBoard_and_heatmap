// SPDX-License-Identifier: GPL-3.0-only

// ai-note: v2 — captures the ACTUAL text window around the cursor (v1 only kept char counts + a
// fingerprint) plus the absolute cursor offset, so we can read the word now occupying any tracked
// field offset and reconcile each WordSession.finalText. Respects IME_FLAG_NO_PERSONALIZED_LEARNING.
// AI EDIT MAP:
//   capture()           -> WindowProbe(cursorOffset, before, after) or null when blocked/disconnected
//   WindowProbe.wordCoveringOffset(offset) -> the whitespace-delimited word at an absolute offset
package helium314.keyboard.heatmap.learning

import android.view.inputmethod.EditorInfo
import helium314.keyboard.latin.RichInputConnection

object FieldTextProbe_v2 {

    const val MAX_BEFORE_CHARS = 200
    const val MAX_AFTER_CHARS = 120

    /**
     * Text captured around the cursor. [windowStart] is the absolute field offset of the first
     * character of [before]; [cursorOffset] is the absolute offset of the cursor (== windowStart +
     * before.length). [after] continues from the cursor.
     */
    data class WindowProbe(
        val cursorOffset: Int,
        val windowStart: Int,
        val before: String,
        val after: String,
    ) {
        private val combined: String = before + after

        /** The word (run of non-space chars) whose absolute range covers [offset], or null if outside. */
        fun wordCoveringOffset(offset: Int): String? {
            if (combined.isEmpty()) return null
            val local = offset - windowStart
            if (local < 0 || local > combined.length) return null
            // Find the word boundaries around the clamped local index.
            val idx = local.coerceIn(0, combined.length - 1)
            if (combined[idx].isWhitespace() && (idx == local)) {
                // offset sits on a separator -> look just left (trailing edit of previous word).
                if (idx == 0) return null
                if (combined[idx - 1].isWhitespace()) return null
            }
            var start = idx.coerceAtMost(combined.length - 1)
            // If we landed on whitespace (e.g. end boundary), step left onto the word.
            if (combined[start].isWhitespace() && start > 0) start--
            if (combined[start].isWhitespace()) return null
            var l = start
            while (l > 0 && !combined[l - 1].isWhitespace()) l--
            var r = start
            while (r < combined.length - 1 && !combined[r + 1].isWhitespace()) r++
            val word = combined.substring(l, r + 1)
            return word.ifBlank { null }
        }
    }

    @JvmStatic
    fun capture(connection: RichInputConnection?, editorInfo: EditorInfo?): WindowProbe? {
        if (connection == null) return null
        if (editorInfo != null &&
            (editorInfo.imeOptions and EditorInfo.IME_FLAG_NO_PERSONALIZED_LEARNING) != 0
        ) {
            return null
        }
        if (!connection.isConnected()) return null
        val cursor = connection.expectedSelectionStart
        if (cursor < 0) return null
        val before = connection.getTextBeforeCursor(MAX_BEFORE_CHARS, 0)?.toString().orEmpty()
        val after = connection.getTextAfterCursor(MAX_AFTER_CHARS, 0)?.toString().orEmpty()
        return WindowProbe(
            cursorOffset = cursor,
            windowStart = (cursor - before.length).coerceAtLeast(0),
            before = before,
            after = after,
        )
    }
}
