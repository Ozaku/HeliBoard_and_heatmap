// SPDX-License-Identifier: GPL-3.0-only
// ai-note: SharedPreferences keys for Heatmap Smart Keyboard settings screen
package helium314.keyboard.heatmap.learning

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import helium314.keyboard.latin.utils.prefs

object HeatmapLearningSettings_v1 {
    const val PREF_LEARNING_ENABLED = "heatmap_learning_enabled"
    const val PREF_PARAGRAPH_WINDOW_CHARS = "heatmap_paragraph_window_chars"

    const val PARAGRAPH_WINDOW_MIN = 500
    const val PARAGRAPH_WINDOW_MAX = 3000
    const val PARAGRAPH_WINDOW_DEFAULT = 3000
    /** ai-note: word memory — max attempts (kept + erased) in rolling window */
    const val WORD_MEMORY_MAX_WORDS = 300
    /** Light snap hint for slider (user can pick values between snaps). */
    const val PARAGRAPH_WINDOW_SNAP = 100

    fun isLearningEnabled(prefs: SharedPreferences): Boolean =
        prefs.getBoolean(PREF_LEARNING_ENABLED, true)

    fun isLearningEnabled(context: Context): Boolean = isLearningEnabled(context.prefs())

    fun getParagraphWindowChars(prefs: SharedPreferences): Int =
        prefs.getInt(PREF_PARAGRAPH_WINDOW_CHARS, PARAGRAPH_WINDOW_DEFAULT)
            .coerceIn(PARAGRAPH_WINDOW_MIN, PARAGRAPH_WINDOW_MAX)

    fun getParagraphWindowChars(context: Context): Int = getParagraphWindowChars(context.prefs())

    fun setParagraphWindowChars(context: Context, chars: Int) {
        context.prefs().edit {
            putInt(PREF_PARAGRAPH_WINDOW_CHARS, chars.coerceIn(PARAGRAPH_WINDOW_MIN, PARAGRAPH_WINDOW_MAX))
        }
    }
}
