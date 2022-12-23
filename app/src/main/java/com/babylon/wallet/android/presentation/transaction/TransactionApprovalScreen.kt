package com.babylon.wallet.android.presentation.transaction

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.babylon.wallet.android.R
import com.babylon.wallet.android.designsystem.composable.RadixPrimaryButton
import com.babylon.wallet.android.designsystem.theme.BabylonWalletTheme
import com.babylon.wallet.android.designsystem.theme.RadixTheme
import com.babylon.wallet.android.domain.SampleDataProvider
import com.babylon.wallet.android.presentation.common.FullscreenCircularProgressContent
import com.babylon.wallet.android.presentation.common.UiMessage
import com.babylon.wallet.android.presentation.ui.composables.RadixCenteredTopAppBar
import com.babylon.wallet.android.presentation.ui.composables.SnackbarUiMessageHandler

@Composable
fun TransactionApprovalScreen(
    viewModel: TransactionApprovalViewModel,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val state = viewModel.state
    BackHandler(true) {}
    TransactionApprovalContent(
        onBackClick = onBackClick,
        isLoading = state.isLoading,
        isSigning = state.isSigning,
        manifestContent = state.manifestData?.instructions,
        onApproveTransaction = viewModel::approveTransaction,
        modifier = modifier
            .systemBarsPadding()
            .fillMaxWidth(0.9f)
            .fillMaxHeight(0.8f)
            .background(RadixTheme.colors.defaultBackground, shape = RadixTheme.shapes.roundedRectSmall),
        approved = state.approved,
        error = state.error,
        onMessageShown = viewModel::onMessageShown
    )
    LaunchedEffect(Unit) {
        viewModel.oneOffEvent.collect {
            when (it) {
                TransactionApprovalEvent.TransactionRejected -> {
                    onBackClick()
                }
            }
        }
    }
}

@Composable
private fun TransactionApprovalContent(
    onBackClick: () -> Unit,
    isLoading: Boolean,
    isSigning: Boolean,
    manifestContent: String?,
    onApproveTransaction: () -> Unit,
    approved: Boolean,
    error: UiMessage?,
    onMessageShown: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier) {
        Column(
            modifier = modifier,
            horizontalAlignment = Alignment.Start
        ) {
            RadixCenteredTopAppBar(
                title = stringResource(R.string.approve_transaction),
                onBackClick = onBackClick,
                contentColor = RadixTheme.colors.gray1
            )
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(RadixTheme.dimensions.paddingDefault)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(RadixTheme.dimensions.paddingSmall)
            ) {
                AnimatedVisibility(visible = approved) {
                    Icon(painter = painterResource(id = R.drawable.img_dapp_complete), contentDescription = null)
                    Text(
                        text = stringResource(R.string.transaction_approved_success),
                        style = RadixTheme.typography.body2Regular,
                        color = RadixTheme.colors.gray2,
                    )
                }
                AnimatedVisibility(visible = !approved) {
                    manifestContent?.let { manifestContent ->
                        Text(
                            text = manifestContent,
                            style = RadixTheme.typography.body2Regular,
                            color = RadixTheme.colors.gray2,
                        )
                    }
                }
                Spacer(modifier = Modifier.height(100.dp))
            }
        }
        if (isLoading || isSigning) {
            FullscreenCircularProgressContent()
        }
        RadixPrimaryButton(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .padding(RadixTheme.dimensions.paddingDefault),
            text = stringResource(id = R.string.approve_transaction),
            onClick = onApproveTransaction, enabled = !isLoading && !isSigning
        )
        SnackbarUiMessageHandler(message = error) {
            onMessageShown()
        }
    }
}

@Preview(showBackground = true)
@Composable
fun TransactionApprovalContentPreview() {
    BabylonWalletTheme {
        TransactionApprovalContent(
            onBackClick = {},
            isLoading = false,
            isSigning = false,
            manifestContent = SampleDataProvider().sampleManifest().toString(),
            onApproveTransaction = {},
            approved = false,
            error = null,
            onMessageShown = {}
        )
    }
}
