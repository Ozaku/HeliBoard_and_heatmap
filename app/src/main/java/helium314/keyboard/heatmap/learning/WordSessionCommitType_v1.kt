// SPDX-License-Identifier: GPL-3.0-only

// ai-note: mirrors LastComposedWord.COMMIT_TYPE_* for WordSession_v1

package helium314.keyboard.heatmap.learning



import helium314.keyboard.latin.LastComposedWord



enum class WordSessionCommitType_v1 {

    USER_TYPED,

    MANUAL_PICK,

    DECIDED,

    CANCEL_AUTO_CORRECT,

    UNKNOWN,

    ;



    companion object {

        fun fromLatinCommitType(commitType: Int): WordSessionCommitType_v1 = when (commitType) {

            LastComposedWord.COMMIT_TYPE_USER_TYPED_WORD -> USER_TYPED

            LastComposedWord.COMMIT_TYPE_MANUAL_PICK -> MANUAL_PICK

            LastComposedWord.COMMIT_TYPE_DECIDED_WORD -> DECIDED

            LastComposedWord.COMMIT_TYPE_CANCEL_AUTO_CORRECT -> CANCEL_AUTO_CORRECT

            else -> UNKNOWN

        }

    }

}

