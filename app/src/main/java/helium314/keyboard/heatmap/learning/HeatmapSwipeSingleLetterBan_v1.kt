// SPDX-License-Identifier: GPL-3.0-only

// ai-note: Block 3 rule (interim in legacy gesture path) — ban swipe commits of A/I when stroke is multi-key



package helium314.keyboard.heatmap.learning



import helium314.keyboard.keyboard.Key

import helium314.keyboard.keyboard.Keyboard

import helium314.keyboard.latin.SuggestedWords.SuggestedWordInfo

import helium314.keyboard.latin.common.InputPointers

import helium314.keyboard.latin.utils.Log



object HeatmapSwipeSingleLetterBan_v1 {

    private const val TAG = "HeatmapInstr"



    /** Only these one-letter words are banned from swipe decode (tap still allowed). */

    private val BANNED_SWIPE_SINGLE_LETTERS = setOf("a", "i")



    /** Min squared px start→end span to treat as deliberate swipe (not micro-drag on one key). */

    private const val MIN_SWIPE_SPAN_SQ_PX = 40 * 40



    fun isBannedSwipeSingleLetterWord(word: String): Boolean {

        if (word.length != 1) return false

        return word.lowercase() in BANNED_SWIPE_SINGLE_LETTERS

    }



    /**

     * True when stroke likely crossed multiple letter keys or moved far enough to be a real swipe.

     * Micro-drags on one key may still allow A/I (user can tap instead).

     */

    fun strokeImpliesMultiKeyIntent(keyboard: Keyboard, pointers: InputPointers): Boolean {

        val size = pointers.pointerSize

        if (size < 2) return false

        val xs = pointers.xCoordinates

        val ys = pointers.yCoordinates

        val sampleIndices = buildSet {

            add(0)

            add(size / 2)

            add(size - 1)

            if (size > 8) {

                add(size / 4)

                add(3 * size / 4)

            }

        }

        val letterLabels = mutableSetOf<Char>()

        for (index in sampleIndices) {

            primaryLetterLabel(keyboard, xs[index], ys[index])?.let { letterLabels.add(it) }

        }

        if (letterLabels.size >= 2) return true

        val dx = xs[size - 1] - xs[0]

        val dy = ys[size - 1] - ys[0]

        return (dx * dx + dy * dy) >= MIN_SWIPE_SPAN_SQ_PX

    }



    /**

     * Drop leading A/I suggestions when gesture clearly spans multiple keys (e.g. I→T must not be "I").

     */

    fun suppressBannedSingleLetterSwipeLeaders(

        suggestions: MutableList<SuggestedWordInfo>,

        keyboard: Keyboard,

        pointers: InputPointers,

    ) {

        if (suggestions.isEmpty()) return

        if (!strokeImpliesMultiKeyIntent(keyboard, pointers)) return

        var removed = 0

        while (suggestions.isNotEmpty() && isBannedSwipeSingleLetterWord(suggestions[0].mWord)) {

            Log.i(TAG, "swipe ban: drop single-letter leader '${suggestions[0].mWord}' (multi-key stroke)")

            suggestions.removeAt(0)

            removed++

        }

        if (removed > 0 && suggestions.isEmpty()) {

            Log.w(TAG, "swipe ban: all leaders removed; strip may be empty until next suggestion pass")

        }

    }



    private fun primaryLetterLabel(keyboard: Keyboard, x: Int, y: Int): Char? {

        val keys: List<Key> = keyboard.getNearestKeys(x, y) ?: return null

        if (keys.isEmpty()) return null

        val label = keys[0].label ?: return null

        val c = label.firstOrNull() ?: return null

        if (!c.isLetter()) return null

        return c.lowercaseChar()

    }

}

