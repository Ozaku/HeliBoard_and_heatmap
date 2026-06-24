// SPDX-License-Identifier: GPL-3.0-only

// ai-note: Block 2 step 9 — live keyboard snapshot + cached alphabet map for commits



package helium314.keyboard.heatmap.learning



import android.content.Context

import helium314.keyboard.keyboard.KeyboardSwitcher

import helium314.keyboard.latin.settings.Settings



object HeatmapLayoutContext_v2 {

    @Volatile

    private var lastAlphabetSnapshot: HeatmapCoordinateMap_v1.Snapshot? = null



    @JvmStatic

    fun localeTag(context: Context): String = try {

        Settings.getValues().mLocale.toLanguageTag()

    } catch (_: Exception) {

        "und"

    }



    /** IME commit path — capture current alphabet keyboard or reuse last alphabet snapshot. */

    @JvmStatic

    fun captureForCommit(context: Context): HeatmapCoordinateMap_v1.Snapshot? {

        val keyboard = KeyboardSwitcher.getInstance().keyboard

        val fresh = keyboard?.let { HeatmapCoordinateMap_v1.fromKeyboard(it) }

        if (fresh != null) {

            lastAlphabetSnapshot = fresh

            return fresh

        }

        return lastAlphabetSnapshot

    }



    @JvmStatic

    fun layoutHash(context: Context): String =

        captureForCommit(context)?.layoutHash

            ?: HeatmapKeyBoundsStore_v1.readLatestLayoutHash(context)

            ?: HeatmapLayoutContext_v1.LAYOUT_HASH_PLACEHOLDER



    fun formatStatusBlock(context: Context): String {

        val snap = captureForCommit(context)

        val bounds = HeatmapKeyBoundsStore_v1.readBoundsRowCount(context, snap?.localeTag, snap?.layoutHash)

        return buildString {

            append("\n\n— Layout map (Block 2 step 9) —")

            append("\nLocale: ").append(snap?.localeTag ?: localeTag(context))

            append("\nMain layout: ").append(snap?.mainLayoutName ?: "?")

            append("\nLayout hash: ").append(snap?.layoutHash ?: layoutHash(context))

            append("\nLetter keys in map: ").append(snap?.keys?.size ?: 0)

            append("\nPersisted bounds rows: ").append(bounds)

        }

    }

}

