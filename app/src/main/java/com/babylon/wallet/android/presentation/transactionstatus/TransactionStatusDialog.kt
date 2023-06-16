package com.babylon.wallet.android.presentation.transactionstatus

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.babylon.wallet.android.R
import com.babylon.wallet.android.designsystem.theme.RadixTheme
import com.babylon.wallet.android.designsystem.theme.RadixWalletTheme
import com.babylon.wallet.android.presentation.ui.composables.BottomSheetWrapper
import com.babylon.wallet.android.presentation.ui.composables.SomethingWentWrongDialog

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionStatusDialog(
    modifier: Modifier = Modifier,
    viewModel: TransactionStatusDialogViewModel,
    onBackPress: () -> Unit
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val dismissHandler = {
        viewModel.incomingRequestHandled()
        onBackPress()
    }
    BottomSheetWrapper(
        onDismissRequest = dismissHandler
    ) {
        when (val dialogState = state.transactionStatus) {
            TransactionStatus.Completing -> CompletingBottomDialogContent(modifier = modifier)
            is TransactionStatus.Failed -> SomethingWentWrongDialog(
                modifier = modifier,
                onDismissRequest = dismissHandler,
                subtitle = dialogState.errorTextRes?.let { stringResource(id = it) }.orEmpty()
            )
            TransactionStatus.Success -> TransactionStatusContent(modifier = modifier)
        }
    }
}

@Composable
private fun TransactionStatusContent(
    modifier: Modifier = Modifier
) {
    Column(
        modifier
            .fillMaxWidth()
            .background(color = RadixTheme.colors.defaultBackground)
            .padding(RadixTheme.dimensions.paddingLarge),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(RadixTheme.dimensions.paddingDefault)
    ) {
        Image(
            painter = painterResource(
                id = com.babylon.wallet.android.designsystem.R.drawable.check_circle_outline
            ),
            contentDescription = null
        )
        Text(
            text = stringResource(id = R.string.transaction_status_success_title),
            style = RadixTheme.typography.title,
            color = RadixTheme.colors.gray1
        )
        Text(
            text = stringResource(R.string.transaction_status_success_text),
            style = RadixTheme.typography.body1Regular,
            color = RadixTheme.colors.gray1
        )
    }
}

@Composable
private fun CompletingBottomDialogContent(
    modifier: Modifier = Modifier
) {
    Column(
        modifier
            .fillMaxWidth()
            .background(color = RadixTheme.colors.defaultBackground)
            .padding(RadixTheme.dimensions.paddingLarge),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(RadixTheme.dimensions.paddingDefault)
    ) {
        Image(
            painter = painterResource(
                id = com.babylon.wallet.android.designsystem.R.drawable.check_circle_outline
            ),
            alpha = 0.2F,
            contentDescription = null
        )
        Text(
            text = stringResource(com.babylon.wallet.android.R.string.transaction_status_completing_text),
            style = RadixTheme.typography.body1Regular,
            color = RadixTheme.colors.gray1
        )
        Spacer(Modifier.height(36.dp))
    }
}

@Composable
private fun SuccessBottomDialogContent(
    modifier: Modifier = Modifier,
    isFromTransaction: Boolean,
    dAppName: String
) {
    Column(
        modifier
            .fillMaxWidth()
            .background(color = RadixTheme.colors.defaultBackground)
            .padding(RadixTheme.dimensions.paddingLarge),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(RadixTheme.dimensions.paddingDefault)
    ) {
        Image(
            painter = painterResource(
                id = com.babylon.wallet.android.designsystem.R.drawable.check_circle_outline
            ),
            contentDescription = null
        )
        Text(
            text = stringResource(id = R.string.transaction_status_success_title),
            style = RadixTheme.typography.title,
            color = RadixTheme.colors.gray1
        )
        Text(
            text = if (isFromTransaction) {
                stringResource(R.string.transaction_status_success_text)
            } else {
                stringResource(id = R.string.dAppRequest_completion_subtitle, dAppName)
            },
            style = RadixTheme.typography.body1Regular,
            color = RadixTheme.colors.gray1
        )
    }
}

@Preview(showBackground = true)
@Composable
fun SuccessBottomDialogPreview() {
    RadixWalletTheme {
        SuccessBottomDialogContent(isFromTransaction = false, dAppName = "dApp")
    }
}

@Preview(showBackground = true)
@Composable
fun CompletingBottomDialogPreview() {
    RadixWalletTheme {
        CompletingBottomDialogContent()
    }
}
