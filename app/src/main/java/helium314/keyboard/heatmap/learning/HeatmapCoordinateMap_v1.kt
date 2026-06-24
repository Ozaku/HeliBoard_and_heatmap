// SPDX-License-Identifier: GPL-3.0-only

// ai-note: Block 2 step 9 — letter-key bounds + stable layout hash from live Keyboard



package helium314.keyboard.heatmap.learning



import helium314.keyboard.keyboard.Key

import helium314.keyboard.keyboard.Keyboard

import helium314.keyboard.keyboard.KeyboardId

import helium314.keyboard.latin.common.Constants.Subtype.ExtraValue.KEYBOARD_LAYOUT_SET

import java.security.MessageDigest

import java.util.Locale



object HeatmapCoordinateMap_v1 {

    data class KeyBoundsEntry(

        val label: String,

        val keyCode: Int,

        val left: Int,

        val top: Int,

        val right: Int,

        val bottom: Int,

    ) {

        val centerX: Int get() = (left + right) / 2

        val centerY: Int get() = (top + bottom) / 2

        val storageLabel: String get() = label.lowercase(Locale.US)

    }



    data class Snapshot(

        val localeTag: String,

        val layoutSetExtra: String,

        val mainLayoutName: String,

        val elementId: Int,

        val keyboardWidth: Int,

        val keyboardHeight: Int,

        val layoutHash: String,

        val keys: List<KeyBoundsEntry>,

    ) {

        fun keyLabelForFirstLetter(word: String): String? {

            val c = word.firstOrNull() ?: return null

            if (!c.isLetter()) return null

            val want = c.lowercaseChar().toString()

            return keys.firstOrNull { it.storageLabel == want }?.storageLabel

        }



        fun keyAt(x: Int, y: Int): KeyBoundsEntry? =

            keys.firstOrNull { x >= it.left && x < it.right && y >= it.top && y < it.bottom }

    }



    fun fromKeyboard(keyboard: Keyboard): Snapshot? {

        if (!isLetterLayoutElement(keyboard.mId.mElementId)) return null

        val subtype = keyboard.mId.mSubtype

        val localeTag = subtype.locale.toLanguageTag()

        val layoutSetExtra = subtype.getExtraValueOf(KEYBOARD_LAYOUT_SET) ?: ""

        val keys = buildLetterKeyBounds(keyboard)

        if (keys.isEmpty()) return null

        val hash = computeLayoutHash(

            localeTag = localeTag,

            layoutSetExtra = layoutSetExtra,

            elementId = keyboard.mId.mElementId,

            width = keyboard.mOccupiedWidth,

            height = keyboard.mOccupiedHeight,

            keys = keys,

        )

        return Snapshot(

            localeTag = localeTag,

            layoutSetExtra = layoutSetExtra,

            mainLayoutName = subtype.mainLayoutName,

            elementId = keyboard.mId.mElementId,

            keyboardWidth = keyboard.mOccupiedWidth,

            keyboardHeight = keyboard.mOccupiedHeight,

            layoutHash = hash,

            keys = keys,

        )

    }



    internal fun buildLetterKeyBounds(keyboard: Keyboard): List<KeyBoundsEntry> {

        val out = ArrayList<KeyBoundsEntry>()

        for (key in keyboard.sortedKeys) {

            val entry = keyBoundsFromKey(key) ?: continue

            out.add(entry)

        }

        return out.sortedBy { it.storageLabel }

    }



    private fun keyBoundsFromKey(key: Key): KeyBoundsEntry? {

        val label = key.label ?: return null

        if (label.length != 1) return null

        val c = label[0]

        if (!c.isLetter()) return null

        val box = key.hitBox

        return KeyBoundsEntry(

            label = label,

            keyCode = key.code,

            left = box.left,

            top = box.top,

            right = box.right,

            bottom = box.bottom,

        )

    }



    internal fun computeLayoutHash(

        localeTag: String,

        layoutSetExtra: String,

        elementId: Int,

        width: Int,

        height: Int,

        keys: List<KeyBoundsEntry>,

    ): String {

        val sb = StringBuilder()

        sb.append(localeTag).append('|')

        sb.append(layoutSetExtra).append('|')

        sb.append(elementId).append('|')

        sb.append(width).append('x').append(height).append('|')

        for (k in keys.sortedBy { it.storageLabel }) {

            sb.append(k.storageLabel).append('@')

                .append(k.left).append(',').append(k.top).append(',')

                .append(k.right).append(',').append(k.bottom).append(';')

        }

        val digest = MessageDigest.getInstance("SHA-256").digest(sb.toString().toByteArray(Charsets.UTF_8))

        val hex = digest.joinToString("") { b -> "%02x".format(b) }

        return "lh_$hex"

    }



    private fun isLetterLayoutElement(elementId: Int): Boolean = when (elementId) {

        KeyboardId.ELEMENT_ALPHABET,

        KeyboardId.ELEMENT_ALPHABET_MANUAL_SHIFTED,

        KeyboardId.ELEMENT_ALPHABET_AUTOMATIC_SHIFTED,

        KeyboardId.ELEMENT_ALPHABET_SHIFT_LOCKED,

        KeyboardId.ELEMENT_ALPHABET_SHIFT_LOCK_SHIFTED,

        -> true

        else -> false

    }

}

