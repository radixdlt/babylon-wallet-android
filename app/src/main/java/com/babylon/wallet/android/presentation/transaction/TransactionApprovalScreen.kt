package com.babylon.wallet.android.presentation.transaction

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.babylon.wallet.android.R
import com.babylon.wallet.android.designsystem.theme.BabylonWalletTheme
import com.babylon.wallet.android.designsystem.theme.RadixTheme
import com.babylon.wallet.android.presentation.ui.composables.RadixCenteredTopAppBar

@Composable
fun TransactionApprovalScreen(
    viewModel: TransactionApprovalViewModel,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    viewModel.state
    TransactionApprovalContent(
        onBackClick = onBackClick,
        modifier = modifier
            .systemBarsPadding()
            .fillMaxSize()
            .background(RadixTheme.colors.defaultBackground)
    )
}

@Composable
private fun TransactionApprovalContent(
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.Start
    ) {
        RadixCenteredTopAppBar(
            title = stringResource(R.string.settings),
            onBackClick = onBackClick,
            contentColor = RadixTheme.colors.gray1
        )
    }
}

@Preview(showBackground = true)
@Composable
fun TransactionApprovalContentPreview() {
    BabylonWalletTheme {
        TransactionApprovalContent(
            onBackClick = {},
        )
    }
}
