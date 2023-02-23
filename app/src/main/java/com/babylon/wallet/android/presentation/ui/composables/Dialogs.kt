package com.babylon.wallet.android.presentation.ui.composables

import androidx.compose.foundation.background
import androidx.compose.material.AlertDialog
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import com.babylon.wallet.android.R
import com.babylon.wallet.android.designsystem.composable.RadixTextButton
import com.babylon.wallet.android.designsystem.theme.RadixTheme

@Composable
fun BasicPromptAlertDialog(
    finish: (accepted: Boolean) -> Unit,
    title: @Composable () -> Unit,
    text: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    confirmText: String = stringResource(id = R.string.confirm),
    dismissText: String = stringResource(id = R.string.cancel),
) {
    AlertDialog(
        modifier = modifier
            .background(RadixTheme.colors.defaultBackground, shape = RadixTheme.shapes.roundedRectSmall)
            .clip(RadixTheme.shapes.roundedRectSmall),
        onDismissRequest = { finish(false) },
        confirmButton = {
            RadixTextButton(text = confirmText, onClick = { finish(true) })
        },
        dismissButton = {
            RadixTextButton(text = dismissText, onClick = { finish(false) })
        },
        title = {
            title()
        },
        text = {
            text()
        }
    )
}

@Composable
fun NotSecureAlertDialog(
    finish: (accepted: Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    BasicPromptAlertDialog(
        modifier = modifier,
        finish = finish,
        title = {
            Text(
                text = stringResource(id = R.string.please_confirm_dialog_title),
                style = RadixTheme.typography.body2Header,
                color = RadixTheme.colors.gray1
            )
        },
        text = {
            Text(
                text = stringResource(id = R.string.please_confirm_dialog_body),
                style = RadixTheme.typography.body2Regular,
                color = RadixTheme.colors.gray2
            )
        }
    )
}
