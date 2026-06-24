// SPDX-License-Identifier: GPL-3.0-only

// ai-note: 5s countdown before Yes enables — reset all heatmap training data



package helium314.keyboard.settings.dialogs



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

fun HeatmapLearningResetConfirmDialog_v1(

    onDismissRequest: () -> Unit,

    onConfirmed: () -> Unit,

) {

    var secondsLeft by remember { mutableIntStateOf(COUNTDOWN_SECONDS) }

    LaunchedEffect(Unit) {

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

            Text(

                stringResource(R.string.heatmap_reset_confirm_title),

                fontWeight = FontWeight.Bold,

            )

        },

        content = {

            Text(

                stringResource(R.string.heatmap_reset_confirm_message),

                style = MaterialTheme.typography.bodyLarge,

            )

        },

        confirmButtonText = yesLabel,

        cancelButtonText = stringResource(R.string.heatmap_reset_confirm_no),

        checkOk = { secondsLeft <= 0 },

    )

}

