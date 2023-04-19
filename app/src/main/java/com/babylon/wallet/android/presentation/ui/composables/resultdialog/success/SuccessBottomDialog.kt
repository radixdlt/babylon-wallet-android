package com.babylon.wallet.android.presentation.ui.composables.resultdialog.success

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.babylon.wallet.android.R
import com.babylon.wallet.android.designsystem.theme.RadixTheme
import com.babylon.wallet.android.designsystem.theme.RadixWalletTheme
import com.babylon.wallet.android.presentation.ui.composables.BottomSheetWrapper
import com.babylon.wallet.android.presentation.ui.composables.resultdialog.ResultBottomDialogViewModel

@Composable
fun SuccessBottomDialog(
    modifier: Modifier = Modifier,
    viewModel: ResultBottomDialogViewModel,
    requestId: String,
    isFromTransaction: Boolean,
    dAppName: String,
    onBackPress: () -> Unit
) {
    val dismissHandler = {
        viewModel.incomingRequestHandled(requestId)
        onBackPress()
    }
    BottomSheetWrapper(
        onDismissRequest = dismissHandler
    ) {
        SuccessBottomDialogContent(
            isFromTransaction = isFromTransaction,
            dAppName = dAppName,
            modifier = modifier
        )
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
            text = stringResource(id = R.string.success),
            style = RadixTheme.typography.title,
            color = RadixTheme.colors.gray1
        )
        Text(
            text = if (isFromTransaction) {
                stringResource(R.string.transaction_approved_success)
            } else {
                stringResource(id = R.string.request_complete, dAppName)
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
