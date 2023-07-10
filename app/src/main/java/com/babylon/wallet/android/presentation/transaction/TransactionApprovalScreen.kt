@file:OptIn(ExperimentalMaterialApi::class, ExperimentalMaterialApi::class)

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
import androidx.compose.material.ModalBottomSheetState
import androidx.compose.material.ModalBottomSheetValue
import androidx.compose.material.rememberModalBottomSheetState
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
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
import com.babylon.wallet.android.domain.SampleDataProvider
import com.babylon.wallet.android.domain.model.DAppWithMetadataAndAssociatedResources
import com.babylon.wallet.android.presentation.common.FullscreenCircularProgressContent
import com.babylon.wallet.android.presentation.dapp.authorized.account.AccountItemUiModel
import com.babylon.wallet.android.presentation.settings.dappdetail.DAppDetailsSheetContent
import com.babylon.wallet.android.presentation.status.signing.SigningStatusBottomDialog
import com.babylon.wallet.android.presentation.transaction.composables.ConnectedDAppsContent
import com.babylon.wallet.android.presentation.transaction.composables.DepositAccountContent
import com.babylon.wallet.android.presentation.transaction.composables.FeePayerSelectionSheet
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
import com.babylon.wallet.android.utils.biometricAuthenticate
import com.babylon.wallet.android.utils.findFragmentActivity
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private const val PAYER_DIALOG_CLOSE_DELAY = 300L

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun TransactionApprovalScreen(
    modifier: Modifier = Modifier,
    viewModel: TransactionApprovalViewModel,
    viewModel2: TransactionApprovalViewModel2,
    onDismiss: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val state by viewModel.state.collectAsStateWithLifecycle()
    val state2 by viewModel2.state.collectAsStateWithLifecycle()

    val modalBottomSheetState = rememberModalBottomSheetState(
        initialValue = ModalBottomSheetValue.Hidden,
        skipHalfExpanded = true
    )
    BackHandler {
        if (modalBottomSheetState.isVisible) {
            scope.launch {
                modalBottomSheetState.hide()
            }
        } else {
            viewModel.onBackClick()
        }
    }
    TransactionPreviewContent(
        onBackClick = viewModel::onBackClick,
        state = state,
        onApproveTransaction = viewModel::approveTransaction,
        onMessageShown = viewModel::onMessageShown,
        modifier = modifier,
        onGuaranteesApplyClick = viewModel::onGuaranteesApplyClick,
        onGuaranteesCloseClick = viewModel::onGuaranteesCloseClick,
        promptForGuaranteesClick = viewModel::promptForGuaranteesClick,
        onDAppClick = viewModel::onDAppClick,
        onGuaranteeValueChanged = viewModel::onGuaranteeValueChanged,
        onPayerSelected = viewModel::onPayerSelected,
        onPayerConfirmed = {
            scope.launch {
                modalBottomSheetState.hide()
                delay(PAYER_DIALOG_CLOSE_DELAY)
                viewModel.onPayerConfirmed()
            }
        },
        modalBottomSheetState = modalBottomSheetState,
        resetBottomSheetMode = viewModel::resetBottomSheetMode
    )
    LaunchedEffect(Unit) {
        viewModel.oneOffEvent.collect { event ->
            when (event) {
                TransactionApprovalEvent.Dismiss -> {
                    onDismiss()
                }
                TransactionApprovalEvent.SelectFeePayer -> {
                    scope.launch {
                        modalBottomSheetState.show()
                    }
                }
            }
        }
    }
}

@Suppress("CyclomaticComplexMethod")
@Composable
@OptIn(ExperimentalMaterialApi::class)
private fun TransactionPreviewContent(
    onBackClick: () -> Unit,
    state: TransactionUiState,
    onApproveTransaction: () -> Unit,
    onMessageShown: () -> Unit,
    modifier: Modifier = Modifier,
    onGuaranteesApplyClick: () -> Unit,
    onGuaranteesCloseClick: () -> Unit,
    promptForGuaranteesClick: () -> Unit,
    onDAppClick: (DAppWithMetadataAndAssociatedResources) -> Unit,
    onGuaranteeValueChanged: (Pair<String, GuaranteesAccountItemUiModel>) -> Unit,
    onPayerSelected: (AccountItemUiModel) -> Unit,
    onPayerConfirmed: () -> Unit,
    modalBottomSheetState: ModalBottomSheetState,
    resetBottomSheetMode: () -> Unit
) {
    var showNotSecuredDialog by remember { mutableStateOf(false) }
    var signingStateDismissed by remember { mutableStateOf(false) }
    var showRawManifest by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    if (modalBottomSheetState.currentValue != ModalBottomSheetValue.Hidden) {
        DisposableEffect(Unit) {
            onDispose {
                resetBottomSheetMode()
            }
        }
    }
    BackHandler(enabled = modalBottomSheetState.isVisible) {
        scope.launch {
            modalBottomSheetState.hide()
        }
    }

    DefaultModalSheetLayout(
        modifier = modifier
            .navigationBarsPadding()
            .background(RadixTheme.colors.defaultBackground)
            .fillMaxSize(),
        sheetState = modalBottomSheetState,
        sheetContent = {
            BottomSheetContent(
                bottomSheetViewMode = state.bottomSheetViewMode,
                feePayerCandidates = state.feePayerCandidates,
                onPayerSelected = onPayerSelected,
                onPayerConfirmed = onPayerConfirmed,
                guaranteesAccounts = state.guaranteesAccounts,
                onGuaranteesCloseClick = {
                    scope.launch {
                        modalBottomSheetState.hide()
                    }
                    onGuaranteesCloseClick()
                },
                onGuaranteesApplyClick = {
                    scope.launch {
                        modalBottomSheetState.hide()
                    }
                    onGuaranteesApplyClick()
                },
                onGuaranteeValueChanged = {
                    onGuaranteeValueChanged(it)
                },
                onCloseFeePayerSheet = {
                    scope.launch {
                        modalBottomSheetState.hide()
                    }
                },
                onCloseDAppSheet = {
                    scope.launch {
                        modalBottomSheetState.hide()
                    }
                }
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
                    onBackEnabled = !state.isSigning
                )
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(RadixTheme.dimensions.paddingXSmall)
                        .verticalScroll(rememberScrollState()),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(RadixTheme.dimensions.paddingSmall)
                ) {
                    AnimatedVisibility(visible = !state.isLoading) {
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
                                text = stringResource(R.string.transactionReview_title),
                                style = RadixTheme.typography.title,
                                color = RadixTheme.colors.gray1,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                            )

                            TransactionMessageContent(transactionMessage = state.transactionMessage)

                            WithdrawAccountContent(
                                withdrawAccountsMap = state.withdrawingAccountsMap
                            )

                            StrokeLine(height = 40.dp)

                            ConnectedDAppsContent(
                                connectedDApps = state.connectedDApps,
                                onDAppClick = {
                                    onDAppClick(it)
                                    scope.launch {
                                        modalBottomSheetState.show()
                                    }
                                }
                            )

                            StrokeLine()

                            DepositAccountContent(
                                depositAccountsMap = state.depositingAccountsMap,
                                shouldPromptForGuarantees = state.shouldPromptForGuarantees,
                                promptForGuarantees = {
                                    promptForGuaranteesClick()
                                    scope.launch {
                                        modalBottomSheetState.show()
                                    }
                                }
                            )

                            PresentingProofsContent(presentingProofs = state.presentingProofs)

                            Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingDefault))

                            NetworkFeeContent(
                                networkFee = state.networkFee,
                                isNetworkCongested = false
                            )

                            Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingDefault))

                            val context = LocalContext.current
                            RadixPrimaryButton(
                                text = stringResource(id = R.string.transactionReview_approveButtonTitle),
                                onClick = {
                                    if (state.isDeviceSecure) {
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
                                modifier = Modifier
                                    .fillMaxWidth(),
                                enabled = !state.isLoading && !state.isSigning && state.canApprove &&
                                    state.bottomSheetViewMode != BottomSheetMode.FeePayerSelection,
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
            if (state.isLoading || state.isSigning) {
                FullscreenCircularProgressContent(addOverlay = true, clickable = true)
            }
            if (!signingStateDismissed && state.isSigning && state.signingState != null) {
                SigningStatusBottomDialog(onDismissDialogClick = {
                    signingStateDismissed = true
                }, signingState = state.signingState)
            }
            SnackbarUiMessageHandler(
                message = state.error,
                onMessageShown = onMessageShown
            )
        }
    }

    if (showRawManifest) {
        RawTransactionContent(
            manifestContent = state.manifestString,
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
private fun BottomSheetContent(
    bottomSheetViewMode: BottomSheetMode,
    feePayerCandidates: ImmutableList<AccountItemUiModel>,
    onPayerSelected: (AccountItemUiModel) -> Unit,
    onPayerConfirmed: () -> Unit,
    guaranteesAccounts: ImmutableList<GuaranteesAccountItemUiModel>,
    onGuaranteesCloseClick: () -> Unit,
    onGuaranteesApplyClick: () -> Unit,
    onGuaranteeValueChanged: (Pair<String, GuaranteesAccountItemUiModel>) -> Unit,
    onCloseFeePayerSheet: () -> Unit,
    onCloseDAppSheet: () -> Unit
) {
    when (bottomSheetViewMode) {
        BottomSheetMode.FeePayerSelection -> {
            FeePayerSelectionSheet(
                modifier = Modifier.fillMaxWidth(),
                accounts = feePayerCandidates,
                onClose = onCloseFeePayerSheet,
                onPayerSelected = onPayerSelected,
                onPayerConfirmed = onPayerConfirmed
            )
        }
        BottomSheetMode.Guarantees -> {
            GuaranteesSheet(
                modifier = Modifier.fillMaxWidth(),
                guaranteesAccounts = guaranteesAccounts,
                onClose = onGuaranteesCloseClick,
                onApplyClick = onGuaranteesApplyClick,
                onGuaranteeValueChanged = onGuaranteeValueChanged
            )
        }
        is BottomSheetMode.DApp -> {
            DAppDetailsSheetContent(
                onBackClick = onCloseDAppSheet,
                dappName = bottomSheetViewMode.dApp.dAppWithMetadata.name.orEmpty(),
                dappWithMetadata = bottomSheetViewMode.dApp.dAppWithMetadata,
                associatedFungibleTokens = bottomSheetViewMode.dApp.fungibleResources.toPersistentList(),
                associatedNonFungibleTokens = bottomSheetViewMode.dApp.nonFungibleResources.toPersistentList()
            )
        }
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
                text = stringResource(id = R.string.common_ok),
                onClick = finish
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun TransactionPreviewContentPreview() {
    RadixWalletTheme {
        with(SampleDataProvider()) {
            TransactionPreviewContent(
                onBackClick = {},
                state = TransactionUiState(
                    isLoading = false,
                    isSigning = false,
                    error = null,
                    isDeviceSecure = false,
                    canApprove = true,
                    transactionMessage = "Message",
                    networkFee = "0.1",
                    manifestString = "",
                    presentingProofs = persistentListOf(
                        PresentingProofUiModel("", "Proof"),
                        PresentingProofUiModel("", "Proof")
                    ),
                    connectedDApps = persistentListOf(
                        sampleDAppWithResources()
                    ),
                    withdrawingAccounts = persistentListOf(
                        TransactionAccountItemUiModel(
                            accountAddress = "account_tdx_19jd32jd3928jd3892jd329",
                            displayName = "My Savings Account",
                            tokenSymbol = "XRD",
                            tokenAmount = "689.203",
                            appearanceID = 1,
                            iconUrl = "",
                            shouldPromptForGuarantees = false,
                            guaranteedAmount = "689.203",
                            guaranteedPercentAmount = "100"
                        )
                    ),
                    depositingAccounts = persistentListOf(
                        TransactionAccountItemUiModel(
                            accountAddress = "account_tdx_19jd32jd3928jd3892jd329",
                            displayName = "My Savings Account",
                            tokenSymbol = "XRD",
                            tokenAmount = "689.203",
                            appearanceID = 1,
                            iconUrl = "",
                            shouldPromptForGuarantees = true,
                            guaranteedAmount = "689.203",
                            guaranteedPercentAmount = "100"
                        ),
                        TransactionAccountItemUiModel(
                            accountAddress = "account_tdx_19jd32jd3928jd3892jd39",
                            displayName = "My second Savings Account",
                            tokenSymbol = "XRD",
                            tokenAmount = "689.203",
                            appearanceID = 1,
                            iconUrl = "",
                            shouldPromptForGuarantees = true,
                            guaranteedAmount = "689.203",
                            guaranteedPercentAmount = "100"
                        )
                    ),
                    guaranteesAccounts = persistentListOf(),
                ),
                onApproveTransaction = {},
                onMessageShown = {},
                onGuaranteesApplyClick = {},
                onGuaranteesCloseClick = {},
                promptForGuaranteesClick = {},
                onDAppClick = {},
                onGuaranteeValueChanged = {},
                onPayerSelected = {},
                onPayerConfirmed = {},
                modalBottomSheetState = rememberModalBottomSheetState(
                    initialValue = ModalBottomSheetValue.Hidden,
                    skipHalfExpanded = true
                ),
                resetBottomSheetMode = {}
            )
        }
    }
}
