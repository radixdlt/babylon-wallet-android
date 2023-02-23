package com.babylon.wallet.android.presentation.transaction

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.babylon.wallet.android.R
import com.babylon.wallet.android.designsystem.composable.RadixPrimaryButton
import com.babylon.wallet.android.designsystem.theme.RadixTheme
import com.babylon.wallet.android.designsystem.theme.RadixWalletTheme
import com.babylon.wallet.android.domain.SampleDataProvider
import com.babylon.wallet.android.presentation.common.FullscreenCircularProgressContent
import com.babylon.wallet.android.presentation.common.UiMessage
import com.babylon.wallet.android.presentation.ui.composables.NotSecureAlertDialog
import com.babylon.wallet.android.presentation.ui.composables.RadixCenteredTopAppBar
import com.babylon.wallet.android.presentation.ui.composables.SnackbarUiMessageHandler
import com.babylon.wallet.android.utils.biometricAuthenticate
import com.babylon.wallet.android.utils.findFragmentActivity

@Composable
fun TransactionApprovalScreen(
    viewModel: TransactionApprovalViewModel,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val state = viewModel.state
    BackHandler(true) {}
    TransactionApprovalContent(
        onBackClick = viewModel::onBackClick,
        isLoading = state.isLoading,
        isSigning = state.isSigning,
        manifestContent = state.manifestString,
        onApproveTransaction = viewModel::approveTransaction,
        modifier = modifier
            .navigationBarsPadding()
            .fillMaxSize()
            .background(RadixTheme.colors.defaultBackground),
        approved = state.approved,
        error = state.error,
        onMessageShown = viewModel::onMessageShown,
        isDeviceSecure = state.isDeviceSecure,
        canApprove = state.canApprove
    )
    LaunchedEffect(Unit) {
        viewModel.oneOffEvent.collect {
            when (it) {
                TransactionApprovalEvent.NavigateBack -> {
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
    manifestContent: String,
    onApproveTransaction: () -> Unit,
    approved: Boolean,
    error: UiMessage?,
    onMessageShown: () -> Unit,
    modifier: Modifier = Modifier,
    isDeviceSecure: Boolean,
    canApprove: Boolean,
) {
    var showNotSecuredDialog by remember { mutableStateOf(false) }
    Box(modifier = modifier) {
        Column(
            modifier = Modifier.fillMaxSize(),
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
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(RadixTheme.dimensions.paddingSmall)
                    ) {
                        Icon(painter = painterResource(id = R.drawable.img_dapp_complete), contentDescription = null)
                        Text(
                            text = stringResource(R.string.transaction_approved_success),
                            style = RadixTheme.typography.body2Regular,
                            color = RadixTheme.colors.gray2,
                        )
                    }
                }
                AnimatedVisibility(visible = !approved) {
                    if (manifestContent.isNotEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    RadixTheme.colors.gray4,
                                    RadixTheme.shapes.roundedRectDefault
                                )
                                .padding(RadixTheme.dimensions.paddingMedium)
                        ) {
                            Text(
                                text = manifestContent,
                                style = RadixTheme.typography.body2HighImportance,
                                color = RadixTheme.colors.gray2,
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(100.dp))
            }
        }
        if (isLoading || isSigning) {
            FullscreenCircularProgressContent()
        }
        val context = LocalContext.current
        AnimatedVisibility(
            visible = !approved,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(RadixTheme.dimensions.paddingDefault)
        ) {
            RadixPrimaryButton(
                modifier = Modifier
                    .fillMaxWidth(),
                text = stringResource(id = R.string.approve_transaction),
                onClick = {
                    if (isDeviceSecure) {
                        context.findFragmentActivity()?.let { activity ->
                            activity.biometricAuthenticate(true) { authenticatedSuccessfully ->
                                if (authenticatedSuccessfully) {
                                    onApproveTransaction()
                                }
                            }
                        }
                    } else {
                        showNotSecuredDialog = true
                    }
                },
                enabled = !isLoading && !isSigning && canApprove
            )
        }
        SnackbarUiMessageHandler(message = error) {
            onMessageShown()
        }
    }
    if (showNotSecuredDialog) {
        NotSecureAlertDialog(finish = {
            showNotSecuredDialog = false
            if (it) {
                onApproveTransaction()
            }
        })
    }
}

@Preview(showBackground = true)
@Composable
fun TransactionApprovalContentPreview() {
    RadixWalletTheme {
        TransactionApprovalContent(
            onBackClick = {},
            isLoading = false,
            isSigning = false,
            manifestContent = SampleDataProvider().sampleManifest().toString(),
            onApproveTransaction = {},
            approved = false,
            error = null,
            onMessageShown = {},
            isDeviceSecure = false,
            canApprove = true
        )
    }
}
