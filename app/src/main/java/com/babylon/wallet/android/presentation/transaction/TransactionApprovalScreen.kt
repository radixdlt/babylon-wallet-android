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
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.ModalBottomSheetValue
import androidx.compose.material.rememberModalBottomSheetState
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.babylon.wallet.android.R
import com.babylon.wallet.android.designsystem.composable.RadixPrimaryButton
import com.babylon.wallet.android.designsystem.composable.RadixTextButton
import com.babylon.wallet.android.designsystem.theme.RadixTheme
import com.babylon.wallet.android.designsystem.theme.RadixWalletTheme
import com.babylon.wallet.android.presentation.common.FullscreenCircularProgressContent
import com.babylon.wallet.android.presentation.common.UiMessage
import com.babylon.wallet.android.presentation.transaction.composables.ConnectedDAppsContent
import com.babylon.wallet.android.presentation.transaction.composables.DepositAccountContent
import com.babylon.wallet.android.presentation.transaction.composables.GuaranteesSheet
import com.babylon.wallet.android.presentation.transaction.composables.NetworkFeeContent
import com.babylon.wallet.android.presentation.transaction.composables.PresentingProofsContent
import com.babylon.wallet.android.presentation.transaction.composables.StrokeLine
import com.babylon.wallet.android.presentation.transaction.composables.TransactionMessageContent
import com.babylon.wallet.android.presentation.transaction.composables.TransactionPreviewHeader
import com.babylon.wallet.android.presentation.transaction.composables.WithdrawAccountContent
import com.babylon.wallet.android.presentation.ui.composables.DefaultModalSheetLayout
import com.babylon.wallet.android.presentation.ui.composables.NotSecureAlertDialog
import com.babylon.wallet.android.presentation.ui.composables.SnackbarUiMessageHandler
import com.babylon.wallet.android.presentation.ui.composables.resultdialog.completing.CompletingBottomDialog
import com.babylon.wallet.android.utils.biometricAuthenticate
import com.babylon.wallet.android.utils.findFragmentActivity
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.launch

@Composable
fun TransactionApprovalScreen(
    modifier: Modifier = Modifier,
    viewModel: TransactionApprovalViewModel,
    onBackClick: () -> Unit,
    showSuccessDialog: (requestId: String) -> Unit,
    showErrorDialog: (requestId: String, errorTextRes: Int) -> Unit
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    BackHandler(true) {}

    TransactionPreviewContent(
        modifier = modifier,
        onBackClick = viewModel::onBackClick,
        isLoading = state.isLoading,
        isSigning = state.isSigning,
        onApproveTransaction = viewModel::approveTransaction,
        error = state.error,
        onMessageShown = viewModel::onMessageShown,
        isDeviceSecure = state.isDeviceSecure,
        canApprove = state.canApprove,
        transactionMessage = state.transactionMessage,
        networkFee = state.networkFee,
        rawManifestContent = state.manifestString,
        presentingProofs = state.presentingProofs,
        connectedDApps = state.connectedDApps,
        withdrawingAccounts = state.withdrawingAccounts,
        depositingAccounts = state.depositingAccounts,
        guaranteesAccounts = state.guaranteesAccounts,
        onGuaranteesApplyClick = viewModel::onGuaranteesApplyClick,
        onGuaranteeValueChanged = viewModel::onGuaranteeValueChanged
    )

    LaunchedEffect(Unit) {
        viewModel.oneOffEvent.collect { event ->
            when (event) {
                TransactionApprovalEvent.NavigateBack -> {
                    onBackClick()
                }
                is TransactionApprovalEvent.FlowCompletedWithSuccess -> {
                    onBackClick()
                    showSuccessDialog(event.requestId)
                }
                is TransactionApprovalEvent.FlowCompletedWithError -> {
                    onBackClick()
                    showErrorDialog(event.requestId, event.errorTextRes)
                }
            }
        }
    }
}

@Composable
@OptIn(ExperimentalMaterialApi::class)
private fun TransactionPreviewContent(
    onBackClick: () -> Unit,
    isLoading: Boolean,
    isSigning: Boolean,
    onApproveTransaction: () -> Unit,
    error: UiMessage?,
    onMessageShown: () -> Unit,
    modifier: Modifier = Modifier,
    isDeviceSecure: Boolean,
    canApprove: Boolean,
    transactionMessage: String,
    networkFee: String,
    rawManifestContent: String,
    presentingProofs: ImmutableList<PresentingProofUiModel>,
    connectedDApps: ImmutableList<ConnectedDAppsUiModel>,
    withdrawingAccounts: ImmutableList<PreviewAccountItemsUiModel>,
    depositingAccounts: ImmutableList<PreviewAccountItemsUiModel>,
    guaranteesAccounts: ImmutableList<GuaranteesAccountItemUiModel>,
    onGuaranteesApplyClick: () -> Unit,
    onGuaranteeValueChanged: (Pair<String, GuaranteesAccountItemUiModel>) -> Unit
) {
    var showNotSecuredDialog by remember { mutableStateOf(false) }
    var showRawManifest by remember { mutableStateOf(false) }

    val bottomSheetState =
        rememberModalBottomSheetState(initialValue = ModalBottomSheetValue.Hidden, skipHalfExpanded = true)
    val scope = rememberCoroutineScope()

    BackHandler(enabled = bottomSheetState.isVisible) {
        scope.launch {
            bottomSheetState.hide()
        }
    }

    DefaultModalSheetLayout(
        modifier = modifier
            .navigationBarsPadding()
            .background(RadixTheme.colors.defaultBackground)
            .fillMaxSize(),
        sheetState = bottomSheetState,
        sheetContent = {
            GuaranteesSheet(
                modifier = Modifier.fillMaxWidth(),
                guaranteesAccounts = guaranteesAccounts,
                onClose = {
                    scope.launch {
                        bottomSheetState.hide()
                    }
                },
                onApplyClick = { ->
                    scope.launch {
                        bottomSheetState.hide()
                    }
                    onGuaranteesApplyClick()
                },
                onGuaranteeValueChanged = onGuaranteeValueChanged
            )
        }
    ) {
        Box(
            modifier = Modifier.navigationBarsPadding()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(RadixTheme.colors.gray5)
                    .padding(RadixTheme.dimensions.paddingDefault),
                horizontalAlignment = Alignment.Start
            ) {
                TransactionPreviewHeader(
                    onBackClick = onBackClick,
                    onRawManifestClick = { showRawManifest = !showRawManifest },
                    onBackEnabled = !isSigning
                )
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(RadixTheme.dimensions.paddingXSmall)
                        .verticalScroll(rememberScrollState()),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(RadixTheme.dimensions.paddingSmall)
                ) {
                    AnimatedVisibility(visible = !isLoading) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize(),
                            horizontalAlignment = Alignment.Start
                        ) {
                            Text(
                                modifier = Modifier
                                    .padding(
                                        vertical = RadixTheme.dimensions.paddingLarge,
                                        horizontal = RadixTheme.dimensions.paddingDefault
                                    ),
                                text = stringResource(R.string.review_your_transaction),
                                style = RadixTheme.typography.title,
                                color = RadixTheme.colors.gray1,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                            )

                            TransactionMessageContent(transactionMessage = transactionMessage)

                            WithdrawAccountContent(previewAccounts = withdrawingAccounts)

                            StrokeLine(height = 40.dp)

                            ConnectedDAppsContent(connectedDApps = connectedDApps)

                            StrokeLine()

                            DepositAccountContent(
                                previewAccounts = depositingAccounts,
                                promptForGuarantees = {
                                    scope.launch {
                                        bottomSheetState.show()
                                    }
                                }
                            )

                            PresentingProofsContent(presentingProofs = presentingProofs)

                            Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingDefault))

                            NetworkFeeContent(
                                networkFee = networkFee,
                                isNetworkCongested = false
                            )

                            Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingDefault))

                            val context = LocalContext.current
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
                                enabled = !isLoading && !isSigning && canApprove,
                                icon = {
                                    Icon(
                                        painter = painterResource(
                                            id = com.babylon.wallet.android.designsystem.R.drawable.ic_lock
                                        ),
                                        contentDescription = ""
                                    )
                                }
                            )
                        }
                    }
                }
            }
            if (isLoading) {
                FullscreenCircularProgressContent()
            }
            if (isSigning) {
                CompletingBottomDialog()
            }
            SnackbarUiMessageHandler(message = error) {
                onMessageShown()
            }
        }
    }

    if (showRawManifest) {
        RawTransactionContent(
            manifestContent = rawManifestContent,
            finish = {
                showRawManifest = !showRawManifest
            }
        )
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

@Composable
private fun RawTransactionContent(
    manifestContent: String,
    finish: () -> Unit,
) {
    Dialog(onDismissRequest = finish) {
        Column(
            modifier = Modifier
                .background(
                    RadixTheme.colors.defaultBackground,
                    shape = RadixTheme.shapes.roundedRectSmall
                )
                .clip(RadixTheme.shapes.roundedRectSmall),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Column(
                modifier = Modifier
                    .padding(RadixTheme.dimensions.paddingDefault)
                    .verticalScroll(rememberScrollState())
                    .weight(weight = 1f, fill = false)
            ) {
                Text(
                    text = manifestContent,
                    style = RadixTheme.typography.body2Header,
                    color = RadixTheme.colors.gray1
                )
            }

            RadixTextButton(
                text = stringResource(id = R.string.ok),
                onClick = finish
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun TransactionPreviewContentPreview() {
    RadixWalletTheme {
        TransactionPreviewContent(
            onBackClick = {},
            isLoading = false,
            isSigning = false,
            transactionMessage = "Message",
            networkFee = "0.1",
            rawManifestContent = "",
            presentingProofs = persistentListOf(
                PresentingProofUiModel("", "Proof"),
                PresentingProofUiModel("", "Proof")
            ),
            connectedDApps = persistentListOf(
                ConnectedDAppsUiModel("", "DApp"),
                ConnectedDAppsUiModel("", "DApp")
            ),
            withdrawingAccounts = persistentListOf(
                PreviewAccountItemsUiModel(
                    address = "account_tdx_19jd32jd3928jd3892jd329",
                    accountName = "My Savings Account",
                    appearanceID = 1,
                    accounts = listOf(
                        TransactionAccountItemUiModel(
                            address = "account_tdx_19jd32jd3928jd3892jd329",
                            displayName = "My Savings Account",
                            tokenSymbol = "XRD",
                            tokenQuantity = "689.203",
                            appearanceID = 1,
                            iconUrl = "",
                            isTokenAmountVisible = true,
                            shouldPromptForGuarantees = false,
                            guaranteedQuantity = "689.203",
                            guaranteedPercentAmount = "100"
                        )
                    )
                )
            ),
            depositingAccounts = persistentListOf(
                PreviewAccountItemsUiModel(
                    address = "account_tdx_19jd32jd3928jd3892jd329",
                    accountName = "My Savings Account",
                    appearanceID = 1,
                    accounts = listOf(
                        TransactionAccountItemUiModel(
                            address = "account_tdx_19jd32jd3928jd3892jd329",
                            displayName = "My Savings Account",
                            tokenSymbol = "XRD",
                            tokenQuantity = "689.203",
                            appearanceID = 1,
                            iconUrl = "",
                            isTokenAmountVisible = true,
                            shouldPromptForGuarantees = true,
                            guaranteedQuantity = "689.203",
                            guaranteedPercentAmount = "100"
                        ),
                        TransactionAccountItemUiModel(
                            address = "account_tdx_19jd32jd3928jd3892jd39",
                            displayName = "My second Savings Account",
                            tokenSymbol = "XRD",
                            tokenQuantity = "689.203",
                            appearanceID = 1,
                            iconUrl = "",
                            isTokenAmountVisible = true,
                            shouldPromptForGuarantees = true,
                            guaranteedQuantity = "689.203",
                            guaranteedPercentAmount = "100"
                        )
                    )
                )
            ),
            guaranteesAccounts = persistentListOf(
                PreviewAccountItemsUiModel(
                    address = "account_tdx_19jd32jd3928jd3892jd329",
                    accountName = "My Savings Account",
                    appearanceID = 1,
                    accounts = listOf(
                        TransactionAccountItemUiModel(
                            address = "account_tdx_19jd32jd3928jd3892jd329",
                            displayName = "My Savings Account",
                            tokenSymbol = "XRD",
                            tokenQuantity = "689.203",
                            appearanceID = 1,
                            iconUrl = "",
                            isTokenAmountVisible = true,
                            shouldPromptForGuarantees = true,
                            guaranteedQuantity = "689.203",
                            guaranteedPercentAmount = "100"
                        ),
                        TransactionAccountItemUiModel(
                            address = "account_tdx_19jd32jd3928jd3892jd39",
                            displayName = "My second Savings Account",
                            tokenSymbol = "XRD",
                            tokenQuantity = "689.203",
                            appearanceID = 1,
                            iconUrl = "",
                            isTokenAmountVisible = true,
                            shouldPromptForGuarantees = true,
                            guaranteedQuantity = "689.203",
                            guaranteedPercentAmount = "100"
                        )
                    )
                )
            ).toGuaranteesAccountsUiModel(),
            onApproveTransaction = {},
//            approved = false,
            error = null,
            onMessageShown = {},
            isDeviceSecure = false,
            canApprove = true,
            onGuaranteesApplyClick = {},
            onGuaranteeValueChanged = {}
        )
    }
}
