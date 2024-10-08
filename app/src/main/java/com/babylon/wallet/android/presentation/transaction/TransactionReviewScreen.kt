package com.babylon.wallet.android.presentation.transaction

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.exclude
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.babylon.wallet.android.R
import com.babylon.wallet.android.designsystem.theme.RadixTheme
import com.babylon.wallet.android.designsystem.theme.RadixWalletTheme
import com.babylon.wallet.android.domain.model.TransferableAsset
import com.babylon.wallet.android.domain.model.messages.DappToWalletInteraction
import com.babylon.wallet.android.domain.model.messages.RemoteEntityID
import com.babylon.wallet.android.domain.model.messages.TransactionRequest
import com.babylon.wallet.android.domain.usecases.TransactionFeePayers
import com.babylon.wallet.android.presentation.common.FullscreenCircularProgressContent
import com.babylon.wallet.android.presentation.dialogs.info.GlossaryItem
import com.babylon.wallet.android.presentation.settings.approveddapps.dappdetail.UnknownAddressesSheetContent
import com.babylon.wallet.android.presentation.transaction.TransactionReviewViewModel.State
import com.babylon.wallet.android.presentation.transaction.composables.AccountDepositSettingsTypeContent
import com.babylon.wallet.android.presentation.transaction.composables.FeePayerSelectionSheet
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
import com.babylon.wallet.android.presentation.transaction.model.AccountWithPredictedGuarantee
import com.babylon.wallet.android.presentation.ui.composables.BasicPromptAlertDialog
import com.babylon.wallet.android.presentation.ui.composables.DefaultModalSheetLayout
import com.babylon.wallet.android.presentation.ui.composables.RadixSnackbarHost
import com.babylon.wallet.android.presentation.ui.composables.ReceiptEdge
import com.babylon.wallet.android.presentation.ui.composables.SlideToSignButton
import com.babylon.wallet.android.presentation.ui.composables.SnackbarUIMessage
import com.babylon.wallet.android.presentation.ui.composables.utils.SyncSheetState
import com.radixdlt.sargon.Address
import com.radixdlt.sargon.NetworkId
import com.radixdlt.sargon.annotation.UsesSampleValues
import com.radixdlt.sargon.extensions.asGeneral
import com.radixdlt.sargon.extensions.orZero
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toPersistentList
import rdx.works.core.domain.DApp
import rdx.works.core.domain.TransactionManifestData
import rdx.works.core.domain.TransactionManifestData.TransactionMessage
import rdx.works.core.domain.TransactionVersion
import rdx.works.core.domain.resources.Resource
import java.util.UUID

@Composable
fun TransactionReviewScreen(
    modifier: Modifier = Modifier,
    viewModel: TransactionReviewViewModel,
    onDismiss: () -> Unit,
    onTransferableFungibleClick: (asset: TransferableAsset.Fungible) -> Unit,
    onTransferableNonFungibleClick: (asset: TransferableAsset.NonFungible, Resource.NonFungibleResource.Item?) -> Unit,
    onDAppClick: (DApp) -> Unit,
    onInfoClick: (GlossaryItem) -> Unit
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.oneOffEvent.collect { event ->
            when (event) {
                Event.Dismiss -> onDismiss()
            }
        }
    }

    TransactionPreviewContent(
        onBackClick = viewModel::onBackClick,
        state = state,
        onApproveTransaction = viewModel::onApproveTransaction,
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
        onTransferableNonFungibleClick = onTransferableNonFungibleClick,
        onChangeFeePayerClick = viewModel::onChangeFeePayerClick,
        onSelectFeePayerClick = viewModel::onSelectFeePayerClick,
        onPayerChanged = viewModel::onPayerChanged,
        onPayerSelected = viewModel::onPayerSelected,
        onFeePaddingAmountChanged = viewModel::onFeePaddingAmountChanged,
        onFeePayerSelectionDismiss = viewModel::onFeePayerSelectionDismissRequest,
        onTipPercentageChanged = viewModel::onTipPercentageChanged,
        onViewDefaultModeClick = viewModel::onViewDefaultModeClick,
        onViewAdvancedModeClick = viewModel::onViewAdvancedModeClick,
        dismissTransactionErrorDialog = viewModel::dismissTerminalErrorDialog,
        onAcknowledgeRawTransactionWarning = viewModel::onAcknowledgeRawTransactionWarning,
        onInfoClick = onInfoClick
    )
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
    onTransferableNonFungibleClick: (asset: TransferableAsset.NonFungible, Resource.NonFungibleResource.Item?) -> Unit,
    onChangeFeePayerClick: () -> Unit,
    onSelectFeePayerClick: () -> Unit,
    onPayerChanged: (TransactionFeePayers.FeePayerCandidate) -> Unit,
    onPayerSelected: () -> Unit,
    onFeePaddingAmountChanged: (String) -> Unit,
    onFeePayerSelectionDismiss: () -> Unit,
    onTipPercentageChanged: (String) -> Unit,
    onViewDefaultModeClick: () -> Unit,
    onViewAdvancedModeClick: () -> Unit,
    dismissTransactionErrorDialog: () -> Unit,
    onAcknowledgeRawTransactionWarning: () -> Unit,
    onInfoClick: (GlossaryItem) -> Unit
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
                                        onUnknownAddressesClick(componentAddresses.map { it.asGeneral() }.toPersistentList())
                                    },
                                    onTransferableFungibleClick = onTransferableFungibleClick,
                                    onNonTransferableFungibleClick = onTransferableNonFungibleClick
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
                                    onNonTransferableFungibleClick = onTransferableNonFungibleClick,
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

                        PresentingProofsContent(
                            badges = state.previewType.badges.toPersistentList(),
                            onInfoClick = onInfoClick,
                            onClick = { badge ->
                                when (val resource = badge.resource) {
                                    is Resource.FungibleResource -> onTransferableFungibleClick(
                                        TransferableAsset.Fungible.Token(
                                            amount = resource.ownedAmount.orZero(),
                                            resource = resource,
                                            isNewlyCreated = false
                                        )
                                    )

                                    is Resource.NonFungibleResource -> onTransferableNonFungibleClick(
                                        TransferableAsset.NonFungible.NFTAssets(
                                            resource = resource,
                                            isNewlyCreated = false
                                        ),
                                        resource.items.firstOrNull()
                                    )
                                }
                            }
                        )

                        NetworkFeeContent(
                            modifier = Modifier.padding(horizontal = RadixTheme.dimensions.paddingXXLarge),
                            fees = state.transactionFees,
                            noFeePayerSelected = state.noFeePayerSelected,
                            insufficientBalanceToPayTheFee = state.isBalanceInsufficientToPayTheFee,
                            isSelectedFeePayerInvolvedInTransaction = state.isSelectedFeePayerInvolvedInTransaction,
                            isNetworkFeeLoading = state.isNetworkFeeLoading,
                            onCustomizeClick = onCustomizeClick,
                            onInfoClick = onInfoClick
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
                    sheetState = state.sheetState,
                    transactionFees = state.transactionFees,
                    insufficientBalanceToPayTheFee = state.isBalanceInsufficientToPayTheFee,
                    isSelectedFeePayerInvolvedInTransaction = state.isSelectedFeePayerInvolvedInTransaction,
                    onCloseBottomSheetClick = onCloseBottomSheetClick,
                    onGuaranteesApplyClick = onGuaranteesApplyClick,
                    onGuaranteeValueChanged = onGuaranteeValueChanged,
                    onGuaranteeValueIncreased = onGuaranteeValueIncreased,
                    onGuaranteeValueDecreased = onGuaranteeValueDecreased,
                    onChangeFeePayerClick = onChangeFeePayerClick,
                    onSelectFeePayerClick = onSelectFeePayerClick,
                    onFeePaddingAmountChanged = onFeePaddingAmountChanged,
                    onTipPercentageChanged = onTipPercentageChanged,
                    onViewDefaultModeClick = onViewDefaultModeClick,
                    onViewAdvancedModeClick = onViewAdvancedModeClick,
                    onInfoClick = onInfoClick
                )
            },
            showDragHandle = true,
            onDismissRequest = onBackClick,
            windowInsets = WindowInsets.systemBars.exclude(WindowInsets.navigationBars)
        )
    }

    val feePayerSheetState = rememberModalBottomSheetState(
        skipPartiallyExpanded = true
    )
    SyncSheetState(
        sheetState = feePayerSheetState,
        isSheetVisible = state.selectedFeePayerInput != null,
        onSheetClosed = onFeePayerSelectionDismiss
    )
    if (state.selectedFeePayerInput != null) {
        FeePayerSelectionSheet(
            input = state.selectedFeePayerInput,
            sheetState = feePayerSheetState,
            onPayerChanged = onPayerChanged,
            onSelectButtonClick = onPayerSelected,
            onDismiss = onFeePayerSelectionDismiss
        )
    }
}

@Composable
private fun BottomSheetContent(
    modifier: Modifier = Modifier,
    sheetState: State.Sheet,
    transactionFees: TransactionFees,
    insufficientBalanceToPayTheFee: Boolean,
    isSelectedFeePayerInvolvedInTransaction: Boolean,
    onCloseBottomSheetClick: () -> Unit,
    onGuaranteesApplyClick: () -> Unit,
    onGuaranteeValueChanged: (AccountWithPredictedGuarantee, String) -> Unit,
    onGuaranteeValueIncreased: (AccountWithPredictedGuarantee) -> Unit,
    onGuaranteeValueDecreased: (AccountWithPredictedGuarantee) -> Unit,
    onChangeFeePayerClick: () -> Unit,
    onSelectFeePayerClick: () -> Unit,
    onFeePaddingAmountChanged: (String) -> Unit,
    onTipPercentageChanged: (String) -> Unit,
    onViewDefaultModeClick: () -> Unit,
    onViewAdvancedModeClick: () -> Unit,
    onInfoClick: (GlossaryItem) -> Unit
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
                onGuaranteeValueDecreased = onGuaranteeValueDecreased,
                onInfoClick = onInfoClick
            )
        }

        is State.Sheet.CustomizeFees -> {
            FeesSheet(
                modifier = modifier,
                state = sheetState,
                transactionFees = transactionFees,
                insufficientBalanceToPayTheFee = insufficientBalanceToPayTheFee,
                isSelectedFeePayerInvolvedInTransaction = isSelectedFeePayerInvolvedInTransaction,
                onClose = onCloseBottomSheetClick,
                onChangeFeePayerClick = onChangeFeePayerClick,
                onSelectFeePayerClick = onSelectFeePayerClick,
                onFeePaddingAmountChanged = onFeePaddingAmountChanged,
                onTipPercentageChanged = onTipPercentageChanged,
                onViewDefaultModeClick = onViewDefaultModeClick,
                onViewAdvancedModeClick = onViewAdvancedModeClick,
                onInfoClick = onInfoClick
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

@Preview(showBackground = true)
@UsesSampleValues
@Composable
fun TransactionPreviewContentPreview() {
    RadixWalletTheme {
        TransactionPreviewContent(
            onBackClick = {},
            state = State(
                request = TransactionRequest(
                    remoteEntityId = RemoteEntityID.ConnectorId(""),
                    interactionId = UUID.randomUUID().toString(),
                    transactionManifestData = TransactionManifestData(
                        instructions = "",
                        networkId = NetworkId.MAINNET,
                        message = TransactionMessage.Public("Hello"),
                        version = TransactionVersion.Default.value
                    ),
                    requestMetadata = DappToWalletInteraction.RequestMetadata.internal(NetworkId.MAINNET)
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
            onTransferableNonFungibleClick = { _, _ -> },
            onGuaranteeValueChanged = { _, _ -> },
            onGuaranteeValueIncreased = {},
            onGuaranteeValueDecreased = {},
            onChangeFeePayerClick = {},
            onSelectFeePayerClick = {},
            onPayerChanged = {},
            onPayerSelected = {},
            onFeePaddingAmountChanged = {},
            onTipPercentageChanged = {},
            onViewDefaultModeClick = {},
            onViewAdvancedModeClick = {},
            dismissTransactionErrorDialog = {},
            onAcknowledgeRawTransactionWarning = {},
            onFeePayerSelectionDismiss = {},
            onInfoClick = {}
        )
    }
}
