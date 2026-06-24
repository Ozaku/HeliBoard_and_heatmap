// SPDX-License-Identifier: GPL-3.0-only

// ai-note: Block 2 step 11 — shared reset preference + countdown dialog wiring

package helium314.keyboard.settings.screens

import android.content.Context
import android.widget.Toast
import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import helium314.keyboard.heatmap.learning.HeatmapLearningReset_v2
import helium314.keyboard.latin.R
import helium314.keyboard.latin.utils.getActivity
import helium314.keyboard.settings.SettingsActivity
import helium314.keyboard.settings.dialogs.HeatmapLearningResetConfirmDialog_v2
import helium314.keyboard.settings.preferences.Preference

@Composable
fun HeatmapResetPreference_v1(
    context: Context,
    @StringRes titleRes: Int,
    @StringRes summaryRes: Int,
    @StringRes confirmTitleRes: Int,
    @StringRes confirmMessageRes: Int,
    @StringRes doneRes: Int,
    layer: HeatmapLearningReset_v2.Layer,
) {
    var showDialog by remember { mutableStateOf(false) }
    if (showDialog) {
        HeatmapLearningResetConfirmDialog_v2(
            titleRes = confirmTitleRes,
            messageRes = confirmMessageRes,
            onDismissRequest = { showDialog = false },
            onConfirmed = {
                showDialog = false
                val ok = HeatmapLearningReset_v2.wipe(context, layer)
                val activity = context.getActivity() as? SettingsActivity
                if (ok) {
                    Toast.makeText(context, doneRes, Toast.LENGTH_LONG).show()
                } else {
                    Toast.makeText(context, R.string.heatmap_reset_failed, Toast.LENGTH_LONG).show()
                }
                activity?.prefChanged()
            },
        )
    }
    Preference(
        name = stringResource(titleRes),
        description = context.getString(summaryRes),
        onClick = { showDialog = true },
    )
}
