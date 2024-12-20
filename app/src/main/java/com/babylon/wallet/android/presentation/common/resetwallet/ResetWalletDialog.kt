package com.babylon.wallet.android.presentation.common.resetwallet

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import com.babylon.wallet.android.R
import com.babylon.wallet.android.designsystem.theme.RadixTheme

@Composable
fun ResetWalletDialog(
    modifier: Modifier = Modifier,
    onConfirm: () -> Unit,
    onDeny: () -> Unit
) {
    AlertDialog(
        modifier = modifier,
        onDismissRequest = onDeny,
        shape = RadixTheme.shapes.roundedRectSmall,
        containerColor = RadixTheme.colors.defaultBackground,
        title = {
            Text(
                text = stringResource(id = R.string.factoryReset_dialog_title),
                style = RadixTheme.typography.body2Header,
                color = RadixTheme.colors.gray1
            )
        },
        text = {
            Text(
                text = stringResource(id = R.string.factoryReset_dialog_message),
                style = RadixTheme.typography.body1Regular,
                color = RadixTheme.colors.gray1
            )
        },
        confirmButton = {
            Text(
                modifier = modifier
                    .clickable(role = Role.Button) { onConfirm() }
                    .padding(
                        horizontal = RadixTheme.dimensions.paddingSmall,
                        vertical = RadixTheme.dimensions.paddingXSmall
                    ),
                text = stringResource(id = R.string.common_confirm),
                color = RadixTheme.colors.red1
            )
        },
        dismissButton = {
            Text(
                modifier = modifier
                    .clickable(role = Role.Button) { onDeny() }
                    .padding(
                        horizontal = RadixTheme.dimensions.paddingSmall,
                        vertical = RadixTheme.dimensions.paddingXSmall
                    ),
                text = stringResource(id = R.string.common_cancel),
                color = RadixTheme.colors.blue2
            )
        }
    )
}
