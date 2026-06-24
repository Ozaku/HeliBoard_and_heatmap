// SPDX-License-Identifier: GPL-3.0-only

package helium314.keyboard.heatmap.learning



import helium314.keyboard.latin.LastComposedWord

import org.junit.Assert.assertEquals

import org.junit.Test



class WordSessionCommitType_v1Test {

    @Test

    fun mapsLatinCommitTypes() {

        assertEquals(

            WordSessionCommitType_v1.USER_TYPED,

            WordSessionCommitType_v1.fromLatinCommitType(LastComposedWord.COMMIT_TYPE_USER_TYPED_WORD),

        )

        assertEquals(

            WordSessionCommitType_v1.MANUAL_PICK,

            WordSessionCommitType_v1.fromLatinCommitType(LastComposedWord.COMMIT_TYPE_MANUAL_PICK),

        )

        assertEquals(

            WordSessionCommitType_v1.DECIDED,

            WordSessionCommitType_v1.fromLatinCommitType(LastComposedWord.COMMIT_TYPE_DECIDED_WORD),

        )

        assertEquals(

            WordSessionCommitType_v1.CANCEL_AUTO_CORRECT,

            WordSessionCommitType_v1.fromLatinCommitType(LastComposedWord.COMMIT_TYPE_CANCEL_AUTO_CORRECT),

        )

        assertEquals(WordSessionCommitType_v1.UNKNOWN, WordSessionCommitType_v1.fromLatinCommitType(-1))

    }



    @Test

    fun inputModeFromGestureFlag() {

        assertEquals(WordSessionInputMode_v1.SWIPE, WordSessionInputMode_v1.fromGestureInput(true))

        assertEquals(WordSessionInputMode_v1.TAP, WordSessionInputMode_v1.fromGestureInput(false))

    }

}

