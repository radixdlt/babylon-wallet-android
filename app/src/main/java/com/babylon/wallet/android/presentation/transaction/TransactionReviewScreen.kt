package com.babylon.wallet.android.presentation.transaction

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SheetState
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.babylon.wallet.android.R
import com.babylon.wallet.android.data.transaction.InteractionState
import com.babylon.wallet.android.designsystem.theme.RadixTheme
import com.babylon.wallet.android.designsystem.theme.RadixWalletTheme
import com.babylon.wallet.android.domain.model.MessageFromDataChannel
import com.babylon.wallet.android.domain.model.TransferableAsset
import com.babylon.wallet.android.domain.userFriendlyMessage
import com.babylon.wallet.android.presentation.common.FullscreenCircularProgressContent
import com.babylon.wallet.android.presentation.settings.approveddapps.dappdetail.UnknownAddressesSheetContent
import com.babylon.wallet.android.presentation.status.signing.FactorSourceInteractionBottomDialog
import com.babylon.wallet.android.presentation.transaction.TransactionReviewViewModel.State
import com.babylon.wallet.android.presentation.transaction.composables.AccountDepositSettingsTypeContent
import com.babylon.wallet.android.presentation.transaction.composables.FeesSheet
import com.babylon.wallet.android.presentation.transaction.composables.GuaranteesSheet
import com.babylon.wallet.android.presentation.transaction.composables.NetworkFeeContent
import com.babylon.wallet.android.presentation.transaction.composables.PoolTypeContent
import com.babylon.wallet.android.presentation.transaction.composables.PresentingProofsContent
import com.babylon.wallet.android.presentation.transaction.composables.RawManifestView
import com.babylon.wallet.android.presentation.transaction.composables.StakeTypeContent
import com.babylon.wallet.android.presentation.transaction.composables.TransactionPreviewHeader
import com.babylon.wallet.android.presentation.transaction.composables.TransferTypeContent
import com.babylon.wallet.android.presentation.transaction.fees.TransactionFees
import com.babylon.wallet.android.presentation.ui.composables.BasicPromptAlertDialog
import com.babylon.wallet.android.presentation.ui.composables.DefaultModalSheetLayout
import com.babylon.wallet.android.presentation.ui.composables.RadixSnackbarHost
import com.babylon.wallet.android.presentation.ui.composables.ReceiptEdge
import com.babylon.wallet.android.presentation.ui.composables.SlideToSignButton
import com.babylon.wallet.android.presentation.ui.composables.SnackbarUIMessage
import com.babylon.wallet.android.utils.biometricAuthenticateSuspend
import com.radixdlt.sargon.Account
import com.radixdlt.sargon.Address
import com.radixdlt.sargon.NetworkId
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.launch
import rdx.works.core.domain.DApp
import rdx.works.core.domain.TransactionManifestData
import rdx.works.core.domain.TransactionManifestData.TransactionMessage
import rdx.works.core.domain.TransactionVersion
import rdx.works.core.domain.resources.Resource

@Composable
fun TransactionReviewScreen(
    modifier: Modifier = Modifier,
    viewModel: TransactionReviewViewModel,
    onDismiss: () -> Unit,
    onTransferableFungibleClick: (asset: TransferableAsset.Fungible) -> Unit,
    onTransferableNonFungibleClick: (asset: TransferableAsset.NonFungible, Resource.NonFungibleResource.Item) -> Unit,
    onDAppClick: (DApp) -> Unit
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current

    TransactionPreviewContent(
        onBackClick = viewModel::onBackClick,
        state = state,
        onApproveTransaction = {
            viewModel.approveTransaction(deviceBiometricAuthenticationProvider = {
                context.biometricAuthenticateSuspend()
            })
        },
        onRawManifestToggle = viewModel::onRawManifestToggle,
        onMessageShown = viewModel::onMessageShown,
        modifier = modifier,
        promptForGuarantees = viewModel::promptForGuaranteesClick,
        onCustomizeClick = viewModel::onCustomizeClick,
        onGuaranteesApplyClick = viewModel::onGuaranteesApplyClick,
        onCloseBottomSheetClick = viewModel::onCloseBottomSheetClick,
        onGuaranteeValueChanged = viewModel::onGuaranteeValueChange,
        onGuaranteeValueIncreased = viewModel::onGuaranteeValueIncreased,
        onGuaranteeValueDecreased = viewModel::onGuaranteeValueDecreased,
        onDAppClick = onDAppClick,
        onUnknownAddressesClick = viewModel::onUnknownAddressesClick,
        onTransferableFungibleClick = onTransferableFungibleClick,
        onNonTransferableFungibleClick = onTransferableNonFungibleClick,
        onChangeFeePayerClick = viewModel::onChangeFeePayerClick,
        onSelectFeePayerClick = viewModel::onSelectFeePayerClick,
        onPayerSelected = viewModel::onPayerSelected,
        onFeePaddingAmountChanged = viewModel::onFeePaddingAmountChanged,
        onTipPercentageChanged = viewModel::onTipPercentageChanged,
        onViewDefaultModeClick = viewModel::onViewDefaultModeClick,
        onViewAdvancedModeClick = viewModel::onViewAdvancedModeClick,
        dismissTransactionErrorDialog = viewModel::dismissTerminalErrorDialog,
        onAcknowledgeRawTransactionWarning = viewModel::onAcknowledgeRawTransactionWarning
    )

    state.interactionState?.let {
        when (it) {
            is InteractionState.Ledger.Error -> {
                BasicPromptAlertDialog(
                    finish = { viewModel.onCancelSigningClick() },
                    message = {
                        Text(text = it.failure.userFriendlyMessage())
                    },
                    confirmText = stringResource(id = R.string.common_ok),
                    dismissText = null
                )
            }

            else -> FactorSourceInteractionBottomDialog(
                modifier = Modifier.fillMaxHeight(0.8f),
                onDismissDialogClick = viewModel::onBackClick,
                interactionState = it
            )
        }
    }

    LaunchedEffect(state.isTransactionDismissed) {
        if (state.isTransactionDismissed) {
            onDismiss()
        }
    }
}

@Suppress("CyclomaticComplexMethod")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TransactionPreviewContent(
    modifier: Modifier = Modifier,
    onBackClick: () -> Unit,
    state: State,
    onApproveTransaction: () -> Unit,
    onRawManifestToggle: () -> Unit,
    onMessageShown: () -> Unit,
    promptForGuarantees: () -> Unit,
    onCustomizeClick: () -> Unit,
    onGuaranteesApplyClick: () -> Unit,
    onCloseBottomSheetClick: () -> Unit,
    onGuaranteeValueChanged: (AccountWithPredictedGuarantee, String) -> Unit,
    onGuaranteeValueIncreased: (AccountWithPredictedGuarantee) -> Unit,
    onGuaranteeValueDecreased: (AccountWithPredictedGuarantee) -> Unit,
    onDAppClick: (DApp) -> Unit,
    onUnknownAddressesClick: (ImmutableList<Address>) -> Unit,
    onTransferableFungibleClick: (asset: TransferableAsset.Fungible) -> Unit,
    onNonTransferableFungibleClick: (asset: TransferableAsset.NonFungible, Resource.NonFungibleResource.Item) -> Unit,
    onChangeFeePayerClick: () -> Unit,
    onSelectFeePayerClick: () -> Unit,
    onPayerSelected: (Account) -> Unit,
    onFeePaddingAmountChanged: (String) -> Unit,
    onTipPercentageChanged: (String) -> Unit,
    onViewDefaultModeClick: () -> Unit,
    onViewAdvancedModeClick: () -> Unit,
    dismissTransactionErrorDialog: () -> Unit,
    onAcknowledgeRawTransactionWarning: () -> Unit
) {
    val modalBottomSheetState = rememberModalBottomSheetState(
        skipPartiallyExpanded = true
    )
    val snackBarHostState = remember { SnackbarHostState() }

    state.error?.let { transactionError ->
        if (transactionError.isTerminalError) {
            BasicPromptAlertDialog(
                finish = {
                    dismissTransactionErrorDialog()
                },
                titleText = transactionError.getTitle(),
                messageText = transactionError.uiMessage.getMessage(),
                confirmText = stringResource(id = R.string.common_ok),
                dismissText = null
            )
        } else {
            SnackbarUIMessage(
                message = transactionError.uiMessage,
                snackbarHostState = snackBarHostState,
                onMessageShown = onMessageShown
            )
        }
    }

    if (state.showRawTransactionWarning) {
        BasicPromptAlertDialog(
            finish = { acknowledged ->
                if (acknowledged) {
                    onAcknowledgeRawTransactionWarning()
                }
            },
            titleText = stringResource(id = R.string.transactionReview_nonConformingManifestWarning_title),
            messageText = stringResource(id = R.string.transactionReview_nonConformingManifestWarning_message),
            confirmText = stringResource(
                id = R.string.common_continue
            ),
            dismissText = null
        )
    }

    BackHandler(onBack = onBackClick)

    SyncSheetState(
        sheetState = modalBottomSheetState,
        isSheetVisible = state.isSheetVisible,
        onSheetClosed = {
            if (state.isSheetVisible) {
                onBackClick()
            }
        }
    )
    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()
    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            TransactionPreviewHeader(
                onBackClick = onBackClick,
                state = state,
                onRawManifestClick = onRawManifestToggle,
                scrollBehavior = scrollBehavior
            )
        },
        snackbarHost = {
            RadixSnackbarHost(
                modifier = Modifier.padding(RadixTheme.dimensions.paddingDefault),
                hostState = snackBarHostState
            )
        },
        containerColor = RadixTheme.colors.defaultBackground
    ) { padding ->
        Box(
            modifier = Modifier
                .padding(padding)
                .background(RadixTheme.colors.gray5)
        ) {
            if (state.isLoading) {
                FullscreenCircularProgressContent()
            } else {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState())
                ) {
                    AnimatedVisibility(
                        visible = state.isRawManifestVisible,
                        enter = fadeIn(),
                        exit = fadeOut()
                    ) {
                        RawManifestView(
                            modifier = Modifier
                                .padding(RadixTheme.dimensions.paddingDefault),
                            manifest = state.rawManifest
                        )
                    }

                    AnimatedVisibility(
                        visible = !state.isRawManifestVisible,
                        enter = fadeIn(),
                        exit = fadeOut()
                    ) {
                        when (val preview = state.previewType) {
                            is PreviewType.None -> {}
                            is PreviewType.UnacceptableManifest -> {
                                return@AnimatedVisibility
                            }

                            is PreviewType.NonConforming -> {}
                            is PreviewType.Transfer.GeneralTransfer -> {
                                TransferTypeContent(
                                    state = state,
                                    preview = preview,
                                    onPromptForGuarantees = promptForGuarantees,
                                    onDAppClick = onDAppClick,
                                    onUnknownComponentsClick = { componentAddresses ->
                                        onUnknownAddressesClick(componentAddresses.map { Address.Component(it) }.toPersistentList())
                                    },
                                    onTransferableFungibleClick = onTransferableFungibleClick,
                                    onNonTransferableFungibleClick = onNonTransferableFungibleClick
                                )
                            }

                            is PreviewType.AccountsDepositSettings -> {
                                AccountDepositSettingsTypeContent(
                                    preview = preview
                                )
                            }

                            is PreviewType.Transfer.Staking -> {
                                StakeTypeContent(
                                    state = state,
                                    onTransferableFungibleClick = onTransferableFungibleClick,
                                    onNonTransferableFungibleClick = onNonTransferableFungibleClick,
                                    onPromptForGuarantees = promptForGuarantees,
                                    previewType = preview
                                )
                            }

                            is PreviewType.Transfer.Pool -> {
                                PoolTypeContent(
                                    state = state,
                                    onTransferableFungibleClick = onTransferableFungibleClick,
                                    onPromptForGuarantees = promptForGuarantees,
                                    previewType = preview,
                                    onDAppClick = onDAppClick,
                                    onUnknownPoolsClick = { pools ->
                                        onUnknownAddressesClick(pools.map { Address.Pool(it.address) }.toPersistentList())
                                    }
                                )
                            }
                        }
                    }

                    Column(modifier = Modifier.background(RadixTheme.colors.defaultBackground)) {
                        ReceiptEdge(color = RadixTheme.colors.gray5)
                        if (state.previewType is PreviewType.Transfer.GeneralTransfer) {
                            PresentingProofsContent(badges = state.previewType.badges.toPersistentList())
                        }

                        NetworkFeeContent(
                            modifier = Modifier.padding(horizontal = RadixTheme.dimensions.paddingXXLarge),
                            fees = state.transactionFees,
                            noFeePayerSelected = state.noFeePayerSelected,
                            insufficientBalanceToPayTheFee = state.isBalanceInsufficientToPayTheFee,
                            isNetworkFeeLoading = state.isNetworkFeeLoading,
                            onCustomizeClick = onCustomizeClick
                        )
                        SlideToSignButton(
                            modifier = Modifier
                                .padding(horizontal = RadixTheme.dimensions.paddingXXLarge)
                                .padding(
                                    top = RadixTheme.dimensions.paddingDefault,
                                    bottom = RadixTheme.dimensions.paddingXXLarge
                                ),
                            enabled = state.isSubmitEnabled,
                            isSubmitting = state.isSubmitting,
                            onSwipeComplete = onApproveTransaction
                        )
                    }
                }
            }
            ReceiptEdge(color = RadixTheme.colors.defaultBackground)
        }
    }
    if (state.isSheetVisible) {
        DefaultModalSheetLayout(
            modifier = modifier.fillMaxSize(),
            sheetState = modalBottomSheetState,
            enableImePadding = true,
            sheetContent = {
                BottomSheetContent(
                    modifier = Modifier.navigationBarsPadding(),
                    sheetState = state.sheetState,
                    transactionFees = state.transactionFees,
                    insufficientBalanceToPayTheFee = state.isBalanceInsufficientToPayTheFee,
                    onCloseBottomSheetClick = onCloseBottomSheetClick,
                    onGuaranteesApplyClick = onGuaranteesApplyClick,
                    onGuaranteeValueChanged = onGuaranteeValueChanged,
                    onGuaranteeValueIncreased = onGuaranteeValueIncreased,
                    onGuaranteeValueDecreased = onGuaranteeValueDecreased,
                    onChangeFeePayerClick = onChangeFeePayerClick,
                    onSelectFeePayerClick = onSelectFeePayerClick,
                    onPayerSelected = onPayerSelected,
                    onFeePaddingAmountChanged = onFeePaddingAmountChanged,
                    onTipPercentageChanged = onTipPercentageChanged,
                    onViewDefaultModeClick = onViewDefaultModeClick,
                    onViewAdvancedModeClick = onViewAdvancedModeClick
                )
            },
            showDragHandle = true,
            onDismissRequest = onBackClick
        )
    }
}

@Composable
private fun BottomSheetContent(
    modifier: Modifier = Modifier,
    sheetState: State.Sheet,
    transactionFees: TransactionFees,
    insufficientBalanceToPayTheFee: Boolean,
    onCloseBottomSheetClick: () -> Unit,
    onGuaranteesApplyClick: () -> Unit,
    onGuaranteeValueChanged: (AccountWithPredictedGuarantee, String) -> Unit,
    onGuaranteeValueIncreased: (AccountWithPredictedGuarantee) -> Unit,
    onGuaranteeValueDecreased: (AccountWithPredictedGuarantee) -> Unit,
    onChangeFeePayerClick: () -> Unit,
    onSelectFeePayerClick: () -> Unit,
    onPayerSelected: (Account) -> Unit,
    onFeePaddingAmountChanged: (String) -> Unit,
    onTipPercentageChanged: (String) -> Unit,
    onViewDefaultModeClick: () -> Unit,
    onViewAdvancedModeClick: () -> Unit
) {
    when (sheetState) {
        is State.Sheet.CustomizeGuarantees -> {
            GuaranteesSheet(
                modifier = modifier,
                state = sheetState,
                onClose = onCloseBottomSheetClick,
                onApplyClick = onGuaranteesApplyClick,
                onGuaranteeValueChanged = onGuaranteeValueChanged,
                onGuaranteeValueIncreased = onGuaranteeValueIncreased,
                onGuaranteeValueDecreased = onGuaranteeValueDecreased
            )
        }

        is State.Sheet.CustomizeFees -> {
            FeesSheet(
                modifier = modifier,
                state = sheetState,
                transactionFees = transactionFees,
                insufficientBalanceToPayTheFee = insufficientBalanceToPayTheFee,
                onClose = onCloseBottomSheetClick,
                onChangeFeePayerClick = onChangeFeePayerClick,
                onSelectFeePayerClick = onSelectFeePayerClick,
                onPayerSelected = onPayerSelected,
                onFeePaddingAmountChanged = onFeePaddingAmountChanged,
                onTipPercentageChanged = onTipPercentageChanged,
                onViewDefaultModeClick = onViewDefaultModeClick,
                onViewAdvancedModeClick = onViewAdvancedModeClick
            )
        }

        is State.Sheet.UnknownAddresses -> {
            UnknownAddressesSheetContent(
                modifier = modifier,
                onBackClick = onCloseBottomSheetClick,
                unknownAddresses = sheetState.unknownAddresses
            )
        }

        is State.Sheet.None -> {}
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SyncSheetState(
    sheetState: SheetState,
    isSheetVisible: Boolean,
    onSheetClosed: () -> Unit,
) {
    val scope = rememberCoroutineScope()

    LaunchedEffect(isSheetVisible) {
        if (isSheetVisible) {
            scope.launch { sheetState.show() }
        } else {
            scope.launch { sheetState.hide() }
        }
    }

    LaunchedEffect(sheetState.isVisible) {
        if (!sheetState.isVisible) {
            onSheetClosed()
        }
    }
}

@Preview(showBackground = true)
@Composable
fun TransactionPreviewContentPreview() {
    RadixWalletTheme {
        TransactionPreviewContent(
            onBackClick = {},
            state = State(
                request = MessageFromDataChannel.IncomingRequest.TransactionRequest(
                    remoteConnectorId = "",
                    requestId = "",
                    transactionManifestData = TransactionManifestData(
                        instructions = "",
                        networkId = NetworkId.MAINNET,
                        message = TransactionMessage.Public("Hello"),
                        version = TransactionVersion.Default.value
                    ),
                    requestMetadata = MessageFromDataChannel.IncomingRequest.RequestMetadata.internal(NetworkId.MAINNET)
                ),
                isLoading = false,
                isNetworkFeeLoading = false,
                previewType = PreviewType.NonConforming
            ),
            onApproveTransaction = {},
            onRawManifestToggle = {},
            onMessageShown = {},
            onGuaranteesApplyClick = {},
            onCloseBottomSheetClick = {},
            promptForGuarantees = {},
            onCustomizeClick = {},
            onDAppClick = {},
            onUnknownAddressesClick = {},
            onTransferableFungibleClick = {},
            onNonTransferableFungibleClick = { _, _ -> },
            onGuaranteeValueChanged = { _, _ -> },
            onGuaranteeValueIncreased = {},
            onGuaranteeValueDecreased = {},
            onChangeFeePayerClick = {},
            onSelectFeePayerClick = {},
            onPayerSelected = {},
            onFeePaddingAmountChanged = {},
            onTipPercentageChanged = {},
            onViewDefaultModeClick = {},
            onViewAdvancedModeClick = {},
            dismissTransactionErrorDialog = {},
            onAcknowledgeRawTransactionWarning = {}
        )
    }
}
