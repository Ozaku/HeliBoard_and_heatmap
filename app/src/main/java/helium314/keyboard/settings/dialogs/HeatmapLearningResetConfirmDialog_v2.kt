// SPDX-License-Identifier: GPL-3.0-only

// ai-note: parameterized 5s countdown reset dialog for granular layers (step 11)

package helium314.keyboard.settings.dialogs

import androidx.annotation.StringRes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import helium314.keyboard.latin.R
import kotlinx.coroutines.delay

private const val COUNTDOWN_SECONDS = 5

@Composable
fun HeatmapLearningResetConfirmDialog_v2(
    @StringRes titleRes: Int,
    @StringRes messageRes: Int,
    onDismissRequest: () -> Unit,
    onConfirmed: () -> Unit,
) {
    var secondsLeft by remember(titleRes, messageRes) { mutableIntStateOf(COUNTDOWN_SECONDS) }
    LaunchedEffect(titleRes, messageRes) {
        secondsLeft = COUNTDOWN_SECONDS
        while (secondsLeft > 0) {
            delay(1000L)
            secondsLeft--
        }
    }
    val yesLabel = if (secondsLeft > 0) {
        stringResource(R.string.heatmap_reset_confirm_yes_countdown, secondsLeft)
    } else {
        stringResource(R.string.heatmap_reset_confirm_yes)
    }
    ThreeButtonAlertDialog(
        onDismissRequest = onDismissRequest,
        onConfirmed = onConfirmed,
        scrollContent = true,
        title = {
            Text(stringResource(titleRes), fontWeight = FontWeight.Bold)
        },
        content = {
            Text(stringResource(messageRes), style = MaterialTheme.typography.bodyLarge)
        },
        confirmButtonText = yesLabel,
        cancelButtonText = stringResource(R.string.heatmap_reset_confirm_no),
        checkOk = { secondsLeft <= 0 },
    )
}
