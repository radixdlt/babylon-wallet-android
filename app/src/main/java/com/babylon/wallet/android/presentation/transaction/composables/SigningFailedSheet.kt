package com.babylon.wallet.android.presentation.transaction.composables

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import com.babylon.wallet.android.R
import com.babylon.wallet.android.designsystem.composable.RadixTextButton
import com.babylon.wallet.android.designsystem.theme.RadixTheme
import com.babylon.wallet.android.presentation.transaction.TransactionReviewViewModel.State.Sheet.SigningFailed
import com.babylon.wallet.android.presentation.ui.RadixWalletPreviewTheme
import com.babylon.wallet.android.presentation.ui.composables.BottomDialogHeader
import com.babylon.wallet.android.presentation.ui.composables.RadixBottomBar
import com.babylon.wallet.android.presentation.ui.none

@Composable
fun SigningFailedSheet(
    modifier: Modifier = Modifier,
    state: SigningFailed,
    onDismiss: () -> Unit,
    onRestartSigning: () -> Unit,
    onCancelTransaction: () -> Unit
) {
    Column(
        modifier = modifier
    ) {
        BottomDialogHeader(
            modifier = Modifier.fillMaxWidth(),
            onDismissRequest = onDismiss
        )

        Text(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = RadixTheme.dimensions.paddingXXLarge),
            text = when (state.type) {
                SigningFailed.Type.Transaction -> stringResource(
                    R.string.transactionRecovery_transaction_title
                )
                SigningFailed.Type.PreAuthorization -> stringResource(
                    R.string.transactionRecovery_preAuthorization_title
                )
            },
            style = RadixTheme.typography.title,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingDefault))

        Text(
            modifier = Modifier.fillMaxWidth().padding(horizontal = RadixTheme.dimensions.paddingXXLarge),
            text = when (state.type) {
                SigningFailed.Type.Transaction -> stringResource(R.string.transactionRecovery_transaction_message)
                SigningFailed.Type.PreAuthorization -> stringResource(R.string.transactionRecovery_preAuthorization_message)
            },
            style = RadixTheme.typography.body1Header,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.weight(1f))

        RadixBottomBar(
            text = stringResource(R.string.transactionRecovery_restart),
            onClick = onRestartSigning,
            insets = WindowInsets.none,
            additionalBottomContent = {
                RadixTextButton(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = RadixTheme.dimensions.paddingDefault)
                        .padding(top = RadixTheme.dimensions.paddingSmall),
                    text = when (state.type) {
                        SigningFailed.Type.Transaction -> stringResource(R.string.transactionRecovery_transaction_cancel)
                        SigningFailed.Type.PreAuthorization -> stringResource(R.string.transactionRecovery_preAuthorization_cancel)
                    },
                    onClick = onCancelTransaction
                )
            }
        )
    }
}

@Preview
@Composable
private fun SigningFailedForTransactionSheetPreview() {
    RadixWalletPreviewTheme {
        SigningFailedSheet(
            state = SigningFailed(
                type = SigningFailed.Type.Transaction
            ),
            onDismiss = {},
            onRestartSigning = {},
            onCancelTransaction = {}
        )
    }
}

@Preview
@Composable
private fun SigningFailedForPreAuthSheetPreview() {
    RadixWalletPreviewTheme {
        SigningFailedSheet(
            state = SigningFailed(
                type = SigningFailed.Type.PreAuthorization
            ),
            onDismiss = {},
            onRestartSigning = {},
            onCancelTransaction = {}
        )
    }
}
