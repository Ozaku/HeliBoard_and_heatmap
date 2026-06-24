// SPDX-License-Identifier: GPL-3.0-only

// ai-note: step 1.12 — bounded field text probe; full diff / finalText sync deferred

package helium314.keyboard.heatmap.learning



/** Result of a single InputConnection text probe around the cursor. */

data class FieldTextSnapshot_v1(

    val beforeChars: Int = 0,

    val afterChars: Int = 0,

    /** Cheap change detector until per-word diff lands (1.13+). */

    val fingerprint: Int = 0,

    val skippedReason: String? = null,

) {

    val probed: Boolean get() = skippedReason == null



    fun debugToken(): String = when {

        skippedReason != null -> "skip=$skippedReason"

        else -> "before=$beforeChars after=$afterChars fp=$fingerprint"

    }

}

