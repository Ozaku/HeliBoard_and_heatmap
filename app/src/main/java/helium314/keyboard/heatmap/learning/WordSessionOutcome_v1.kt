// SPDX-License-Identifier: GPL-3.0-only

// ai-note: word memory — kept vs erased/reverted attempts for calibration + future learning

package helium314.keyboard.heatmap.learning

enum class WordSessionOutcome_v1 {

    /** ai-note: committed to text field and still present (last known state) */
    KEPT_IN_FIELD,

    /** ai-note: swipe/tap word erased via backspace before field commit (composing reject) */
    ERASED_BEFORE_COMMIT,

    /** ai-note: committed word reverted/removed from field (backspace revert or word delete) */
    ERASED_FROM_FIELD,

    /** ai-note: autocorrect revert path — committed then explicitly reverted */
    REVERTED_FROM_FIELD,

    /** ai-note: kept word later changed/deleted via a cursor jump or trailing-delete-to-fix-earlier */
    SUPERSEDED_BY_LATER_EDIT,
}
