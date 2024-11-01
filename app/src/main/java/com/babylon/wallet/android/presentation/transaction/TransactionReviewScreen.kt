package com.babylon.wallet.android.presentation.transaction

import androidx.activity.compose.BackHandler
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.babylon.wallet.android.R
import com.babylon.wallet.android.designsystem.theme.RadixTheme
import com.babylon.wallet.android.designsystem.theme.RadixWalletTheme
import com.babylon.wallet.android.domain.usecases.TransactionFeePayers
import com.babylon.wallet.android.presentation.common.FullscreenCircularProgressContent
import com.babylon.wallet.android.presentation.dialogs.info.GlossaryItem
import com.babylon.wallet.android.presentation.model.FungibleAmount
import com.babylon.wallet.android.presentation.model.NonFungibleAmount
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
import com.babylon.wallet.android.presentation.transaction.composables.TransactionPreAuthorizationInfo
import com.babylon.wallet.android.presentation.transaction.composables.TransactionPreviewHeader
import com.babylon.wallet.android.presentation.transaction.composables.TransactionRawManifestToggle
import com.babylon.wallet.android.presentation.transaction.composables.TransferTypeContent
import com.babylon.wallet.android.presentation.transaction.fees.TransactionFees
import com.babylon.wallet.android.presentation.transaction.model.AccountWithTransferables
import com.babylon.wallet.android.presentation.transaction.model.GuaranteeItem
import com.babylon.wallet.android.presentation.transaction.model.Transferable
import com.babylon.wallet.android.presentation.ui.composables.BasicPromptAlertDialog
import com.babylon.wallet.android.presentation.ui.composables.DefaultModalSheetLayout
import com.babylon.wallet.android.presentation.ui.composables.RadixSnackbarHost
import com.babylon.wallet.android.presentation.ui.composables.ReceiptEdge
import com.babylon.wallet.android.presentation.ui.composables.SlideToSignButton
import com.babylon.wallet.android.presentation.ui.composables.SnackbarUIMessage
import com.babylon.wallet.android.presentation.ui.composables.utils.SyncSheetState
import com.babylon.wallet.android.presentation.ui.modifier.applyIf
import com.radixdlt.sargon.Account
import com.radixdlt.sargon.AccountAddress
import com.radixdlt.sargon.Address
import com.radixdlt.sargon.annotation.UsesSampleValues
import com.radixdlt.sargon.extensions.asGeneral
import com.radixdlt.sargon.extensions.orZero
import com.radixdlt.sargon.extensions.toDecimal192
import com.radixdlt.sargon.samples.sampleMainnet
import com.radixdlt.sargon.samples.sampleStokenet
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toPersistentList
import rdx.works.core.domain.DApp
import rdx.works.core.domain.assets.NonFungibleCollection
import rdx.works.core.domain.assets.Token
import rdx.works.core.domain.resources.Resource
import rdx.works.core.domain.resources.sampleMainnet

@Composable
fun TransactionReviewScreen(
    modifier: Modifier = Modifier,
    viewModel: TransactionReviewViewModel,
    onDismiss: () -> Unit,
    onTransferableFungibleClick: (asset: Transferable.FungibleType) -> Unit,
    onTransferableNonFungibleClick: (asset: Transferable.NonFungibleType, Resource.NonFungibleResource.Item?) -> Unit,
    onDAppClick: (DApp) -> Unit,
    onInfoClick: (GlossaryItem) -> Unit
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.oneOffEvent.collect { event ->
            when (event) {
                TransactionReviewViewModel.Event.Dismiss -> onDismiss()
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
        onEditGuaranteesClick = viewModel::onEditGuaranteesClick,
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
        onFeePayerChanged = viewModel::onFeePayerChanged,
        onFeePayerSelected = viewModel::onFeePayerSelected,
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
    onEditGuaranteesClick: () -> Unit,
    onCustomizeClick: () -> Unit,
    onGuaranteesApplyClick: () -> Unit,
    onCloseBottomSheetClick: () -> Unit,
    onGuaranteeValueChanged: (GuaranteeItem, String) -> Unit,
    onGuaranteeValueIncreased: (GuaranteeItem) -> Unit,
    onGuaranteeValueDecreased: (GuaranteeItem) -> Unit,
    onDAppClick: (DApp) -> Unit,
    onUnknownAddressesClick: (ImmutableList<Address>) -> Unit,
    onTransferableFungibleClick: (asset: Transferable.FungibleType) -> Unit,
    onTransferableNonFungibleClick: (asset: Transferable.NonFungibleType, Resource.NonFungibleResource.Item?) -> Unit,
    onChangeFeePayerClick: () -> Unit,
    onSelectFeePayerClick: () -> Unit,
    onFeePayerChanged: (TransactionFeePayers.FeePayerCandidate) -> Unit,
    onFeePayerSelected: () -> Unit,
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
            state.transactionType?.let { transactionType ->
                TransactionPreviewHeader(
                    onBackClick = onBackClick,
                    transactionType = transactionType,
                    isRawManifestToggleVisible = state.isRawManifestToggleVisible,
                    isRawManifestVisible = state.isRawManifestVisible,
                    proposingDApp = state.proposingDApp,
                    onRawManifestClick = onRawManifestToggle,
                    scrollBehavior = scrollBehavior
                )
            }
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
        ) {
            if (state.isLoading) {
                FullscreenCircularProgressContent()
            } else {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState())
                ) {
                    Box(
                        modifier = Modifier
                            .then(
                                if (state.transactionType == State.TransactionType.PreAuthorized) {
                                    Modifier
                                        .padding(horizontal = RadixTheme.dimensions.paddingSmall)
                                        .background(
                                            brush = Brush.verticalGradient(
                                                listOf(RadixTheme.colors.gray5, RadixTheme.colors.gray4)
                                            ),
                                            shape = RadixTheme.shapes.roundedRectMedium
                                        )
                                        .padding(
                                            top = RadixTheme.dimensions.paddingDefault
                                        )
                                } else {
                                    Modifier.background(color = RadixTheme.colors.gray5)
                                }
                            )
                    ) {
                        androidx.compose.animation.AnimatedVisibility(
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

                        androidx.compose.animation.AnimatedVisibility(
                            modifier = Modifier
                                .applyIf(
                                    state.transactionType == State.TransactionType.PreAuthorized,
                                    Modifier.padding(top = RadixTheme.dimensions.paddingSmall)
                                ),
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
                                        onEditGuaranteesClick = onEditGuaranteesClick,
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
                                        onPromptForGuarantees = onEditGuaranteesClick,
                                        previewType = preview
                                    )
                                }

                                is PreviewType.Transfer.Pool -> {
                                    PoolTypeContent(
                                        state = state,
                                        onTransferableFungibleClick = onTransferableFungibleClick,
                                        onPromptForGuarantees = onEditGuaranteesClick,
                                        previewType = preview,
                                        onDAppClick = onDAppClick,
                                        onUnknownPoolsClick = { pools ->
                                            onUnknownAddressesClick(pools.map { Address.Pool(it.address) }.toPersistentList())
                                        }
                                    )
                                }
                            }
                        }

                        if (state.transactionType == State.TransactionType.PreAuthorized) {
                            TransactionRawManifestToggle(
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(end = RadixTheme.dimensions.paddingDefault),
                                isToggleVisible = state.isRawManifestToggleVisible,
                                isToggleOn = state.isRawManifestVisible,
                                onRawManifestClick = onRawManifestToggle
                            )
                        }
                    }

                    Column(modifier = Modifier.background(RadixTheme.colors.defaultBackground)) {
                        if (state.showReceiptEdges) {
                            ReceiptEdge(color = RadixTheme.colors.gray5)
                        }

                        PresentingProofsContent(
                            badges = state.previewType.badges.toPersistentList(),
                            onInfoClick = onInfoClick,
                            onClick = { badge ->
                                when (val resource = badge.resource) {
                                    is Resource.FungibleResource -> onTransferableFungibleClick(
                                        Transferable.FungibleType.Token(
                                            asset = Token(resource = resource),
                                            amount = FungibleAmount.Exact(amount = resource.ownedAmount.orZero()),
                                            isNewlyCreated = false
                                        )
                                    )

                                    is Resource.NonFungibleResource -> onTransferableNonFungibleClick(
                                        Transferable.NonFungibleType.NFTCollection(
                                            asset = NonFungibleCollection(resource),
                                            amount = NonFungibleAmount.Exact(nfts = resource.items),
                                            isNewlyCreated = false
                                        ),
                                        resource.items.firstOrNull()
                                    )
                                }
                            }
                        )

                        state.fees?.let { fees ->
                            NetworkFeeContent(
                                modifier = Modifier.padding(horizontal = RadixTheme.dimensions.paddingXXLarge),
                                fees = fees.transactionFees,
                                properties = fees.properties,
                                isNetworkFeeLoading = fees.isNetworkFeeLoading,
                                onCustomizeClick = onCustomizeClick,
                                onInfoClick = onInfoClick
                            )
                        }

                        state.preAuthorization?.let { preAuthorization ->
                            TransactionPreAuthorizationInfo(
                                modifier = Modifier.padding(RadixTheme.dimensions.paddingSmall),
                                preAuthorization = preAuthorization,
                                onInfoClick = onInfoClick
                            )
                        }

                        SlideToSignButton(
                            modifier = Modifier
                                .padding(
                                    horizontal = if (state.transactionType == State.TransactionType.PreAuthorized) {
                                        RadixTheme.dimensions.paddingDefault
                                    } else {
                                        RadixTheme.dimensions.paddingXXLarge
                                    }
                                )
                                .padding(
                                    top = RadixTheme.dimensions.paddingDefault,
                                    bottom = RadixTheme.dimensions.paddingXXLarge
                                ),
                            title = stringResource(
                                id = if (state.transactionType == State.TransactionType.PreAuthorized) {
                                    R.string.preAuthorizationReview_slideToSign
                                } else {
                                    R.string.interactionReview_slideToSign
                                }
                            ),
                            enabled = state.isSubmitEnabled,
                            isSubmitting = state.isSubmitting,
                            onSwipeComplete = onApproveTransaction
                        )
                    }
                }
            }

            if (state.showReceiptEdges) {
                ReceiptEdge(color = RadixTheme.colors.defaultBackground)
            }
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
            windowInsets = WindowInsets.systemBars
        )
    }

    state.fees?.let { fees ->
        val feePayerSheetState = rememberModalBottomSheetState(
            skipPartiallyExpanded = true
        )
        SyncSheetState(
            sheetState = feePayerSheetState,
            isSheetVisible = fees.selectedFeePayerInput != null,
            onSheetClosed = onFeePayerSelectionDismiss
        )

        if (fees.selectedFeePayerInput != null) {
            FeePayerSelectionSheet(
                input = fees.selectedFeePayerInput,
                sheetState = feePayerSheetState,
                onPayerChanged = onFeePayerChanged,
                onSelectButtonClick = onFeePayerSelected,
                onDismiss = onFeePayerSelectionDismiss
            )
        }
    }
}

@Composable
private fun BottomSheetContent(
    modifier: Modifier = Modifier,
    sheetState: State.Sheet,
    onCloseBottomSheetClick: () -> Unit,
    onGuaranteesApplyClick: () -> Unit,
    onGuaranteeValueChanged: (GuaranteeItem, String) -> Unit,
    onGuaranteeValueIncreased: (GuaranteeItem) -> Unit,
    onGuaranteeValueDecreased: (GuaranteeItem) -> Unit,
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
private fun TransactionPreviewContentPreview(
    @PreviewParameter(TransactionReviewPreviewProvider::class) state: State
) {
    RadixWalletTheme {
        TransactionPreviewContent(
            state = state,
            onBackClick = {},
            onApproveTransaction = {},
            onRawManifestToggle = {},
            onMessageShown = {},
            onGuaranteesApplyClick = {},
            onCloseBottomSheetClick = {},
            onEditGuaranteesClick = {},
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
            onFeePayerChanged = {},
            onFeePayerSelected = {},
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

@UsesSampleValues
class TransactionReviewPreviewProvider : PreviewParameterProvider<State> {

    override val values: Sequence<State>
        get() = sequenceOf(
            State(
                isLoading = false,
                transactionType = State.TransactionType.Regular,
                proposingDApp = State.ProposingDApp.Some(
                    DApp(
                        dAppAddress = AccountAddress.sampleMainnet()
                    )
                ),
                previewType = PreviewType.Transfer.GeneralTransfer(
                    from = listOf(
                        AccountWithTransferables.Owned(
                            account = Account.sampleStokenet(),
                            transferables = listOf(
                                Transferable.FungibleType.Token(
                                    asset = Token(resource = Resource.FungibleResource.sampleMainnet()),
                                    amount = FungibleAmount.Exact("745".toDecimal192()),
                                    isNewlyCreated = false
                                )
                            )
                        )
                    ),
                    to = listOf(
                        AccountWithTransferables.Owned(
                            account = Account.sampleMainnet(),
                            transferables = listOf(
                                Transferable.FungibleType.Token(
                                    asset = Token(resource = Resource.FungibleResource.sampleMainnet()),
                                    amount = FungibleAmount.Exact("745".toDecimal192()),
                                    isNewlyCreated = false
                                )
                            )
                        )
                    ),
                    newlyCreatedGlobalIds = emptyList()
                ),
                fees = State.Fees(
                    isNetworkFeeLoading = false,
                    properties = State.Fees.Properties(),
                    transactionFees = TransactionFees(),
                    selectedFeePayerInput = null
                )
            ),
            State(
                isLoading = false,
                transactionType = State.TransactionType.PreAuthorized,
                proposingDApp = State.ProposingDApp.Some(
                    DApp(
                        dAppAddress = AccountAddress.sampleMainnet()
                    )
                ),
                previewType = PreviewType.Transfer.GeneralTransfer(
                    from = listOf(
                        AccountWithTransferables.Owned(
                            account = Account.sampleStokenet(),
                            transferables = listOf(
                                Transferable.FungibleType.Token(
                                    asset = Token(resource = Resource.FungibleResource.sampleMainnet()),
                                    amount = FungibleAmount.Exact("745".toDecimal192()),
                                    isNewlyCreated = true
                                )
                            )
                        )
                    ),
                    to = listOf(
                        AccountWithTransferables.Owned(
                            account = Account.sampleMainnet(),
                            transferables = listOf(
                                Transferable.FungibleType.Token(
                                    asset = Token(resource = Resource.FungibleResource.sampleMainnet()),
                                    amount = FungibleAmount.Exact("745".toDecimal192()),
                                    isNewlyCreated = true
                                )
                            )
                        )
                    ),
                    newlyCreatedGlobalIds = emptyList()
                ),
                fees = null,
                preAuthorization = State.PreAuthorization(
                    validFor = "23:03 minutes"
                )
            )
        )
}
