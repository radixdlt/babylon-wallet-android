package com.babylon.wallet.android.presentation.ui.composables

import androidx.compose.foundation.background
import androidx.compose.material.AlertDialog
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.babylon.wallet.android.R
import com.babylon.wallet.android.designsystem.composable.RadixTextButton
import com.babylon.wallet.android.designsystem.theme.RadixTheme

@Composable
fun NotSecureAlertDialog(
    show: Boolean,
    finish: (accepted: Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (show) {
        AlertDialog(
            modifier = modifier.background(RadixTheme.colors.defaultBackground),
            onDismissRequest = { finish(false) },
            confirmButton = {
                RadixTextButton(text = stringResource(id = R.string.confirm), onClick = { finish(true) })
            },
            dismissButton = {
                RadixTextButton(text = stringResource(id = R.string.cancel), onClick = { finish(true) })
            },
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
}
