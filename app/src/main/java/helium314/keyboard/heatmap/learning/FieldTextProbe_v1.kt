// SPDX-License-Identifier: GPL-3.0-only

// ai-note: step 1.12 — getTextBefore/After stub; respects IME_FLAG_NO_PERSONALIZED_LEARNING

package helium314.keyboard.heatmap.learning



import android.view.inputmethod.EditorInfo

import helium314.keyboard.latin.RichInputConnection



object FieldTextProbe_v1 {

    const val MAX_BEFORE_CHARS = 120

    const val MAX_AFTER_CHARS = 80



    @JvmStatic

    fun capture(connection: RichInputConnection, editorInfo: EditorInfo?): FieldTextSnapshot_v1 {

        if (editorInfo != null &&

            (editorInfo.imeOptions and EditorInfo.IME_FLAG_NO_PERSONALIZED_LEARNING) != 0

        ) {

            return FieldTextSnapshot_v1(skippedReason = "no_personalized")

        }

        if (!connection.isConnected()) {

            return FieldTextSnapshot_v1(skippedReason = "disconnected")

        }

        val before = connection.getTextBeforeCursor(MAX_BEFORE_CHARS, 0)?.toString().orEmpty()

        val after = connection.getTextAfterCursor(MAX_AFTER_CHARS, 0)?.toString().orEmpty()

        val fingerprint = (before + "|" + after).hashCode()

        return FieldTextSnapshot_v1(

            beforeChars = before.length,

            afterChars = after.length,

            fingerprint = fingerprint,

        )

    }

}

