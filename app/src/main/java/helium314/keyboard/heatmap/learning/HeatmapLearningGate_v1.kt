// SPDX-License-Identifier: GPL-3.0-only

// ai-note: align with HeliBoard InputAttributes + SettingsValues.mIncognitoModeEnabled (password, no-personalized, email)

package helium314.keyboard.heatmap.learning



import android.content.Context

import android.text.InputType

import android.view.inputmethod.EditorInfo

import helium314.keyboard.latin.InputAttributes

import helium314.keyboard.latin.settings.Settings

import helium314.keyboard.latin.utils.InputTypeUtils



object HeatmapLearningGate_v1 {

    /** True when heatmap may record commits, journal, export, and field probes for this editor. */

    @JvmStatic

    fun shouldRecord(context: Context, editorInfo: EditorInfo?): Boolean {

        if (!HeatmapLearningSettings_v2.isLearningEnabled(context)) return false

        if (editorInfo == null) return false

        val attrs = InputAttributes(editorInfo, false, context.packageName)

        return shouldRecord(attrs)

    }



    @JvmStatic

    fun shouldRecord(attrs: InputAttributes): Boolean {

        if (attrs.mIsPasswordField) return false

        if (attrs.mNoLearning) return false

        val variation = attrs.mInputType and InputType.TYPE_MASK_VARIATION

        if (InputTypeUtils.isEmailVariation(variation)) return false

        if (InputTypeUtils.isUriOrEmailType(attrs.mInputType)) return false

        return !isHeliBoardIncognitoActive()

    }



    @JvmStatic

    fun skipReason(editorInfo: EditorInfo?, context: Context): String? {

        if (!HeatmapLearningSettings_v2.isLearningEnabled(context)) return "learning_disabled"

        if (editorInfo == null) return "no_editor"

        val attrs = InputAttributes(editorInfo, false, context.packageName)

        if (attrs.mIsPasswordField) return "password_field"

        if (attrs.mNoLearning) return "no_personalized_learning"

        val variation = attrs.mInputType and InputType.TYPE_MASK_VARIATION

        if (InputTypeUtils.isEmailVariation(variation)) return "email_field"

        if (InputTypeUtils.isUriOrEmailType(attrs.mInputType)) return "uri_or_email_field"

        if (isHeliBoardIncognitoActive()) return "heliboard_incognito"

        return null

    }



    private fun isHeliBoardIncognitoActive(): Boolean = try {

        Settings.getValues().mIncognitoModeEnabled

    } catch (_: Exception) {

        false

    }



    fun formatStatusBlock(context: Context): String {

        val learningOn = HeatmapLearningSettings_v2.isLearningEnabled(context)

        return buildString {

            append("\n\n— Sensitive fields —")

            append("\nLearning master: ").append(if (learningOn) "on" else "off")

            append("\nPassword / private / email: never recorded (HeliBoard InputAttributes).")

            if (learningOn) append("\nBlocked commits log: skip commit (password_field) …")

        }

    }

}

