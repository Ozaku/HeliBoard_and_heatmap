// SPDX-License-Identifier: GPL-3.0-only

// ai-note: tap vs swipe (gesture batch) for WordSession_v1

package helium314.keyboard.heatmap.learning



enum class WordSessionInputMode_v1 {

    TAP,

    SWIPE,

    ;



    companion object {

        /** @param isGestureInput [WordComposer.isBatchMode] before commit */

        fun fromGestureInput(isGestureInput: Boolean): WordSessionInputMode_v1 =

            if (isGestureInput) SWIPE else TAP

    }

}

