package com.babylon.wallet.android.presentation.dapp

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.babylon.wallet.android.R
import com.babylon.wallet.android.designsystem.theme.RadixTheme
import com.babylon.wallet.android.domain.userFriendlyMessage
import com.babylon.wallet.android.presentation.ui.composables.BasicPromptAlertDialog

@Composable
fun DappInteractionFailureDialog(
    dialogState: FailureDialogState,
    onAcknowledgeFailureDialog: () -> Unit,
    modifier: Modifier = Modifier
) {
    when (dialogState) {
        is FailureDialogState.Closed -> {}
        is FailureDialogState.Open -> {
            BasicPromptAlertDialog(
                modifier = modifier,
                finish = {
                    onAcknowledgeFailureDialog()
                },
                title = {
                    Text(
                        text = stringResource(id = R.string.error_dappRequest_invalidRequest),
                        style = RadixTheme.typography.body1Header,
                        color = RadixTheme.colors.gray1
                    )
                },
                message = {
                    Text(
                        text = dialogState.dappRequestException.userFriendlyMessage(),
                        style = RadixTheme.typography.body2Regular,
                        color = RadixTheme.colors.gray1
                    )
                },
                confirmText = stringResource(id = R.string.common_cancel),
                dismissText = null
            )
        }
    }
}
