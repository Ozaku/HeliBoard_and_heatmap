// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.settings.screens

import android.content.Context
import android.widget.Toast
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import helium314.keyboard.heatmap.learning.HeatmapAutocorrectSettings_v1
import helium314.keyboard.heatmap.learning.HeatmapExportSettings_v1
import helium314.keyboard.heatmap.learning.HeatmapLearningReset_v2
import helium314.keyboard.heatmap.swipe.HeatmapLiteralSwipeSettings_v2
import helium314.keyboard.heatmap.swipe.HeatmapSwipeModePolicy_v1
import helium314.keyboard.heatmap.learning.HeatmapLearningSettings_v1
import helium314.keyboard.heatmap.learning.HeatmapWordSlotSession_v7
import helium314.keyboard.latin.R
import helium314.keyboard.latin.utils.Log
import helium314.keyboard.latin.utils.getActivity
import helium314.keyboard.latin.utils.prefs
import helium314.keyboard.settings.Setting
import helium314.keyboard.settings.SearchSettingsScreen
import helium314.keyboard.settings.SettingsActivity
import helium314.keyboard.heatmap.learning.HeatmapExportSave_v1
import helium314.keyboard.heatmap.learning.HeatmapSessionExportWriter_v1
import helium314.keyboard.settings.filePicker
import helium314.keyboard.settings.preferences.Preference
import helium314.keyboard.settings.preferences.SliderPreference
import helium314.keyboard.settings.preferences.SwitchPreference
import helium314.keyboard.latin.utils.Theme
import helium314.keyboard.settings.initPreview
import helium314.keyboard.latin.utils.previewDark

@Composable
fun HeatmapSmartKeyboardScreen(
    onClickBack: () -> Unit,
) {
    val ctx = LocalContext.current
    val activity = ctx.getActivity() as? SettingsActivity
    val b = activity?.prefChanged?.collectAsState()
    if ((b?.value ?: 0) < 0)
        Log.v("irrelevant", "stupid way to trigger recomposition on preference change")
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner, activity) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) activity?.prefChanged()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
    val items = listOf<Any?>(
        HeatmapLearningSettings_v1.PREF_LEARNING_ENABLED,
        HeatmapLearningSettings_v1.PREF_PARAGRAPH_WINDOW_CHARS,
        R.string.heatmap_autocorrect_section,
        HeatmapAutocorrectSettings_v1.PREF_TAP_AUTOCORRECT_ENABLED,
        HeatmapAutocorrectSettings_v1.PREF_SWIPE_AUTOCORRECT_ENABLED,
        HeatmapLiteralSwipeSettings_v2.PREF_USE_LITERAL_SWIPE_ENGINE,
        R.string.heatmap_reset_section,
        "heatmap_reset_swipe_path",
        "heatmap_reset_geometry",
        "heatmap_reset_typo_weights",
        "heatmap_reset_learning",
        R.string.heatmap_diagnostics_section,
        HeatmapExportSettings_v1.PREF_AUTO_FILE_DUMP,
        "heatmap_export_json",
        R.string.heatmap_status_title,
        "heatmap_status",
        "heatmap_privacy_note",
    )
    SearchSettingsScreen(
        onClickBack = onClickBack,
        title = stringResource(R.string.heatmap_smart_keyboard_title),
        settings = items,
    )
}

fun createHeatmapSmartKeyboardSettings(context: Context) = listOf(
    Setting(
        context,
        HeatmapLearningSettings_v1.PREF_LEARNING_ENABLED,
        R.string.heatmap_learning_enabled_title,
        R.string.heatmap_learning_enabled_summary,
    ) {
        SwitchPreference(it, true)
    },
    Setting(context, HeatmapLearningSettings_v1.PREF_PARAGRAPH_WINDOW_CHARS, R.string.heatmap_paragraph_window_title) {
        SliderPreference(
            name = it.title,
            key = it.key,
            default = HeatmapLearningSettings_v1.PARAGRAPH_WINDOW_DEFAULT,
            range = HeatmapLearningSettings_v1.PARAGRAPH_WINDOW_MIN.toFloat()..HeatmapLearningSettings_v1.PARAGRAPH_WINDOW_MAX.toFloat(),
            stepSize = HeatmapLearningSettings_v1.PARAGRAPH_WINDOW_SNAP,
            description = { value ->
                val snapped = (value / HeatmapLearningSettings_v1.PARAGRAPH_WINDOW_SNAP)
                    .toInt() * HeatmapLearningSettings_v1.PARAGRAPH_WINDOW_SNAP
                context.getString(R.string.heatmap_paragraph_window_summary) +
                    ": $value chars (near ${snapped})"
            },
            onConfirmed = { confirmed ->
                HeatmapWordSlotSession_v7.refreshFromSettings(context)
            },
        )
    },
    Setting(
        context,
        HeatmapAutocorrectSettings_v1.PREF_TAP_AUTOCORRECT_ENABLED,
        R.string.heatmap_tap_autocorrect_title,
        R.string.heatmap_tap_autocorrect_summary,
    ) {
        SwitchPreference(it, HeatmapAutocorrectSettings_v1.DEFAULT_TAP_AUTOCORRECT_ENABLED)
    },
    Setting(
        context,
        HeatmapAutocorrectSettings_v1.PREF_SWIPE_AUTOCORRECT_ENABLED,
        R.string.heatmap_swipe_autocorrect_title,
        R.string.heatmap_swipe_autocorrect_summary,
    ) {
        SwitchPreference(it, HeatmapAutocorrectSettings_v1.DEFAULT_SWIPE_AUTOCORRECT_ENABLED)
    },
    Setting(
        context,
        HeatmapLiteralSwipeSettings_v2.PREF_USE_LITERAL_SWIPE_ENGINE,
        R.string.heatmap_swipe_typing_title,
        if (HeatmapSwipeModePolicy_v1.isBlockedByUserGestureLib()) {
            R.string.heatmap_swipe_typing_blocked_summary
        } else {
            R.string.heatmap_swipe_typing_summary
        },
    ) {
        SwitchPreference(
            it,
            HeatmapLiteralSwipeSettings_v2.DEFAULT_HEATMAP_SWIPE_ENABLED,
            allowCheckedChange = { !HeatmapSwipeModePolicy_v1.isBlockedByUserGestureLib() },
        )
    },
    Setting(context, "heatmap_reset_swipe_path", R.string.heatmap_reset_swipe_path_title) {
        HeatmapResetPreference_v1(
            context = context,
            titleRes = R.string.heatmap_reset_swipe_path_title,
            summaryRes = R.string.heatmap_reset_swipe_path_summary,
            confirmTitleRes = R.string.heatmap_reset_swipe_path_confirm_title,
            confirmMessageRes = R.string.heatmap_reset_swipe_path_confirm_message,
            doneRes = R.string.heatmap_reset_swipe_path_done,
            layer = HeatmapLearningReset_v2.Layer.SWIPE_PATH,
        )
    },
    Setting(context, "heatmap_reset_geometry", R.string.heatmap_reset_geometry_title) {
        HeatmapResetPreference_v1(
            context = context,
            titleRes = R.string.heatmap_reset_geometry_title,
            summaryRes = R.string.heatmap_reset_geometry_summary,
            confirmTitleRes = R.string.heatmap_reset_geometry_confirm_title,
            confirmMessageRes = R.string.heatmap_reset_geometry_confirm_message,
            doneRes = R.string.heatmap_reset_geometry_done,
            layer = HeatmapLearningReset_v2.Layer.GEOMETRY,
        )
    },
    Setting(context, "heatmap_reset_typo_weights", R.string.heatmap_reset_typo_weights_title) {
        HeatmapResetPreference_v1(
            context = context,
            titleRes = R.string.heatmap_reset_typo_weights_title,
            summaryRes = R.string.heatmap_reset_typo_weights_summary,
            confirmTitleRes = R.string.heatmap_reset_typo_weights_confirm_title,
            confirmMessageRes = R.string.heatmap_reset_typo_weights_confirm_message,
            doneRes = R.string.heatmap_reset_typo_weights_done,
            layer = HeatmapLearningReset_v2.Layer.TYPO_WEIGHTS,
        )
    },
    Setting(context, "heatmap_reset_learning", R.string.heatmap_reset_learning_title) {
        HeatmapResetPreference_v1(
            context = context,
            titleRes = R.string.heatmap_reset_learning_title,
            summaryRes = R.string.heatmap_reset_learning_summary,
            confirmTitleRes = R.string.heatmap_reset_confirm_title,
            confirmMessageRes = R.string.heatmap_reset_confirm_message,
            doneRes = R.string.heatmap_reset_done,
            layer = HeatmapLearningReset_v2.Layer.ALL,
        )
    },
    Setting(
        context,
        HeatmapExportSettings_v1.PREF_AUTO_FILE_DUMP,
        R.string.heatmap_auto_file_dump_title,
        R.string.heatmap_auto_file_dump_summary,
    ) {
        SwitchPreference(it, HeatmapExportSettings_v1.DEFAULT_AUTO_FILE_DUMP)
    },
    Setting(context, "heatmap_export_json", R.string.heatmap_export_json_title) {
        val activity = context.getActivity() as? SettingsActivity
        val refreshTick = activity?.prefChanged?.collectAsState()?.value ?: 0
        var exportLine by remember(refreshTick) {
            mutableStateOf(HeatmapWordSlotSession_v7.lastExportStatusLine(context))
        }
        LaunchedEffect(refreshTick) {
            exportLine = HeatmapWordSlotSession_v7.lastExportStatusLine(context)
        }
        val savePicker = filePicker { uri ->
            val ok = HeatmapExportSave_v1.writeExportToUri(context, uri)
            if (ok) {
                Toast.makeText(context, R.string.heatmap_export_saved, Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(context, R.string.heatmap_export_failed, Toast.LENGTH_LONG).show()
            }
            exportLine = HeatmapWordSlotSession_v7.lastExportStatusLine(context)
        }
        val saveIntent = remember { HeatmapExportSave_v1.buildSaveIntent(context) }
        Preference(
            name = stringResource(R.string.heatmap_export_json_title),
            description = exportLine + "\n\n" + context.getString(R.string.heatmap_export_json_summary),
            onClick = {
                if (HeatmapSessionExportWriter_v1.ensureShareableFile(context) == null) {
                    Toast.makeText(context, R.string.heatmap_export_failed, Toast.LENGTH_LONG).show()
                    return@Preference
                }
                savePicker.launch(saveIntent)
            },
        )
    },
    Setting(context, "heatmap_status", R.string.heatmap_status_title) {
        val activity = context.getActivity() as? SettingsActivity
        val refreshTick = activity?.prefChanged?.collectAsState()?.value ?: 0
        var statusText by remember(refreshTick) {
            mutableStateOf(HeatmapWordSlotSession_v7.debugSummary(context))
        }
        LaunchedEffect(refreshTick) {
            statusText = HeatmapWordSlotSession_v7.debugSummary(context)
        }
        Preference(
            name = stringResource(R.string.heatmap_status_title),
            description = statusText,
            onClick = {
                HeatmapWordSlotSession_v7.refreshFromSettings(context)
                HeatmapWordSlotSession_v7.onManualDebugRefresh(context)
                activity?.prefChanged()
                statusText = HeatmapWordSlotSession_v7.debugSummary(context)
            },
        )
    },
    Setting(context, "heatmap_privacy_note", R.string.heatmap_privacy_note) {
        Preference(
            name = stringResource(R.string.heatmap_privacy_note),
            onClick = { },
        )
    },
)

@Preview
@Composable
private fun PreviewHeatmapSmartKeyboardScreen() {
    initPreview(LocalContext.current)
    Theme(previewDark) {
        Surface {
            HeatmapSmartKeyboardScreen { }
        }
    }
}
