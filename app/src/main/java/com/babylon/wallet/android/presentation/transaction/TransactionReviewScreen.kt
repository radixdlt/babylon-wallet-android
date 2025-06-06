package com.babylon.wallet.android.presentation.transaction

import androidx.activity.compose.BackHandler
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonColors
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.Typeface
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.babylon.wallet.android.R
import com.babylon.wallet.android.designsystem.theme.RadixTheme
import com.babylon.wallet.android.domain.usecases.TransactionFeePayers
import com.babylon.wallet.android.presentation.common.FullscreenCircularProgressContent
import com.babylon.wallet.android.presentation.dialogs.info.GlossaryItem
import com.babylon.wallet.android.presentation.model.BoundedAmount
import com.babylon.wallet.android.presentation.model.NonFungibleAmount
import com.babylon.wallet.android.presentation.settings.approveddapps.dappdetail.UnknownAddressesSheetContent
import com.babylon.wallet.android.presentation.transaction.TransactionReviewViewModel.State
import com.babylon.wallet.android.presentation.transaction.composables.AccountDepositSettingsTypeContent
import com.babylon.wallet.android.presentation.transaction.composables.DeleteAccountTypeContent
import com.babylon.wallet.android.presentation.transaction.composables.FeePayerSelectionSheet
import com.babylon.wallet.android.presentation.transaction.composables.FeesSheet
import com.babylon.wallet.android.presentation.transaction.composables.GuaranteesSheet
import com.babylon.wallet.android.presentation.transaction.composables.NetworkFeeContent
import com.babylon.wallet.android.presentation.transaction.composables.PresentingProofsContent
import com.babylon.wallet.android.presentation.transaction.composables.SecurifyEntityTypeContent
import com.babylon.wallet.android.presentation.transaction.composables.SigningFailedSheet
import com.babylon.wallet.android.presentation.transaction.composables.TransactionExpirationInfo
import com.babylon.wallet.android.presentation.transaction.composables.TransactionRawManifestToggle
import com.babylon.wallet.android.presentation.transaction.composables.TransactionReviewHeader
import com.babylon.wallet.android.presentation.transaction.composables.TransactionTypeContent
import com.babylon.wallet.android.presentation.transaction.fees.TransactionFees
import com.babylon.wallet.android.presentation.transaction.model.AccountWithTransferables
import com.babylon.wallet.android.presentation.transaction.model.GuaranteeItem
import com.babylon.wallet.android.presentation.transaction.model.InvolvedAccount
import com.babylon.wallet.android.presentation.transaction.model.Transferable
import com.babylon.wallet.android.presentation.ui.RadixWalletPreviewTheme
import com.babylon.wallet.android.presentation.ui.composables.BasicPromptAlertDialog
import com.babylon.wallet.android.presentation.ui.composables.DefaultModalSheetLayout
import com.babylon.wallet.android.presentation.ui.composables.ErrorAlertDialog
import com.babylon.wallet.android.presentation.ui.composables.RadixSnackbarHost
import com.babylon.wallet.android.presentation.ui.composables.ReceiptEdge
import com.babylon.wallet.android.presentation.ui.composables.SlideToSignButton
import com.babylon.wallet.android.presentation.ui.composables.SnackbarUIMessage
import com.babylon.wallet.android.presentation.ui.composables.utils.SyncSheetState
import com.babylon.wallet.android.presentation.ui.modifier.applyIf
import com.babylon.wallet.android.utils.copyToClipboard
import com.radixdlt.sargon.Account
import com.radixdlt.sargon.AccountAddress
import com.radixdlt.sargon.Address
import com.radixdlt.sargon.ManifestEncounteredComponentAddress
import com.radixdlt.sargon.annotation.UsesSampleValues
import com.radixdlt.sargon.extensions.orZero
import com.radixdlt.sargon.extensions.toDecimal192
import com.radixdlt.sargon.samples.sampleMainnet
import com.radixdlt.sargon.samples.sampleStokenet
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList
import kotlinx.collections.immutable.toPersistentList
import rdx.works.core.domain.DApp
import rdx.works.core.domain.assets.NonFungibleCollection
import rdx.works.core.domain.assets.Token
import rdx.works.core.domain.resources.Badge
import rdx.works.core.domain.resources.Resource
import rdx.works.core.domain.resources.sampleMainnet

@Composable
fun TransactionReviewScreen(
    modifier: Modifier = Modifier,
    viewModel: TransactionReviewViewModel,
    onDismiss: () -> Unit,
    onTransferableFungibleClick: (asset: Transferable.FungibleType) -> Unit,
    onTransferableNonFungibleItemClick: (asset: Transferable.NonFungibleType, Resource.NonFungibleResource.Item?) -> Unit,
    onTransferableNonFungibleByAmountClick: (asset: Transferable.NonFungibleType, BoundedAmount) -> Unit,
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

    TransactionReviewContent(
        onBackClick = viewModel::onBackClick,
        state = state,
        onApproveTransaction = viewModel::onSignAndSubmitTransaction,
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
        onTransferableNonFungibleItemClick = onTransferableNonFungibleItemClick,
        onTransferableNonFungibleByAmountClick = onTransferableNonFungibleByAmountClick,
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
        onFailedSigningRestart = viewModel::onFailedSigningRestart,
        onFailedSigningCancel = viewModel::onFailedSigningCancel,
        onInfoClick = onInfoClick
    )
}

@Suppress("CyclomaticComplexMethod")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TransactionReviewContent(
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
    onTransferableNonFungibleItemClick: (asset: Transferable.NonFungibleType, Resource.NonFungibleResource.Item?) -> Unit,
    onTransferableNonFungibleByAmountClick: (asset: Transferable.NonFungibleType, BoundedAmount) -> Unit,
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
    onFailedSigningRestart: () -> Unit,
    onFailedSigningCancel: () -> Unit,
    onInfoClick: (GlossaryItem) -> Unit
) {
    val modalBottomSheetState = rememberModalBottomSheetState(
        skipPartiallyExpanded = true
    )
    val snackBarHostState = remember { SnackbarHostState() }

    state.error?.let { transactionError ->
        if (transactionError.isTerminalError) {
            ErrorAlertDialog(
                cancel = dismissTransactionErrorDialog,
                errorMessage = transactionError.uiMessage,
                cancelMessage = stringResource(id = R.string.common_ok),
                title = transactionError.getTitle()
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
            if (!state.isLoading) {
                TransactionReviewHeader(
                    onBackClick = onBackClick,
                    isPreAuthorization = state.isPreAuthorization,
                    isRawManifestPreviewable = state.isRawManifestToggleInHeader,
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
        containerColor = RadixTheme.colors.background
    ) { padding ->
        if (state.isLoading) {
            FullscreenCircularProgressContent()
        } else {
            Column(
                modifier = Modifier
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .applyIf(
                            condition = state.isPreviewDisplayable,
                            modifier = if (state.isPreAuthorization) {
                                Modifier
                                    .padding(horizontal = RadixTheme.dimensions.paddingSmall)
                                    .background(
                                        brush = if (RadixTheme.config.isDarkTheme) {
                                            SolidColor(RadixTheme.colors.backgroundSecondary)
                                        } else {
                                            Brush.verticalGradient(
                                                listOf(
                                                    RadixTheme.colors.backgroundSecondary,
                                                    RadixTheme.colors.backgroundTertiary
                                                )
                                            )
                                        },
                                        shape = RadixTheme.shapes.roundedRectMedium
                                    )
                            } else {
                                Modifier.background(color = RadixTheme.colors.backgroundSecondary)
                            }
                        )
                ) {
                    androidx.compose.animation.AnimatedVisibility(
                        visible = state.isRawManifestVisible,
                        enter = fadeIn(),
                        exit = fadeOut()
                    ) {
                        Text(
                            modifier = Modifier
                                .padding(
                                    top = 74.dp,
                                    bottom = RadixTheme.dimensions.paddingDefault
                                )
                                .padding(horizontal = RadixTheme.dimensions.paddingDefault),
                            text = state.rawManifest,
                            color = RadixTheme.colors.text,
                            fontSize = 13.sp,
                            fontFamily = FontFamily(Typeface(android.graphics.Typeface.MONOSPACE)),
                        )
                    }

                    androidx.compose.animation.AnimatedVisibility(
                        modifier = Modifier
                            .applyIf(
                                state.isRawManifestToggleInPreview,
                                Modifier.padding(top = RadixTheme.dimensions.paddingLarge)
                            ),
                        visible = !state.isRawManifestVisible,
                        enter = fadeIn(),
                        exit = fadeOut()
                    ) {
                        when (val preview = state.previewType) {
                            is PreviewType.Transaction -> TransactionTypeContent(
                                state = state,
                                previewType = preview,
                                onEditGuaranteesClick = onEditGuaranteesClick,
                                onTransferableFungibleClick = onTransferableFungibleClick,
                                onTransferableNonFungibleItemClick = onTransferableNonFungibleItemClick,
                                onTransferableNonFungibleByAmountClick = onTransferableNonFungibleByAmountClick,
                                onDAppClick = onDAppClick,
                                onUnknownComponentsClick = { onUnknownAddressesClick(it.toImmutableList()) },
                                onInfoClick = onInfoClick
                            )

                            is PreviewType.AccountsDepositSettings -> AccountDepositSettingsTypeContent(
                                preview = preview
                            )

                            is PreviewType.DeleteAccount -> DeleteAccountTypeContent(
                                preview = preview,
                                hiddenResourceIds = state.hiddenResourceIds,
                                onTransferableFungibleClick = onTransferableFungibleClick,
                                onTransferableNonFungibleItemClick = onTransferableNonFungibleItemClick
                            )

                            is PreviewType.SecurifyEntity -> SecurifyEntityTypeContent(
                                preview = preview
                            )

                            else -> {}
                        }
                    }

                    Row(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(
                                top = RadixTheme.dimensions.paddingDefault,
                                bottom = RadixTheme.dimensions.paddingDefault,
                                end = RadixTheme.dimensions.paddingDefault
                            ),
                        horizontalArrangement = Arrangement.spacedBy(RadixTheme.dimensions.paddingSmall),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        androidx.compose.animation.AnimatedVisibility(
                            visible = state.isRawManifestVisible,
                            enter = fadeIn(),
                            exit = fadeOut()
                        ) {
                            val context = LocalContext.current
                            Button(
                                modifier = Modifier.height(40.dp),
                                onClick = {
                                    context.copyToClipboard(
                                        label = "Manifest",
                                        value = state.rawManifest,
                                        successMessage = context.getString(R.string.addressAction_copiedToClipboard)
                                    )
                                },
                                shape = RadixTheme.shapes.roundedRectSmall,
                                elevation = null,
                                colors = ButtonColors(
                                    containerColor = RadixTheme.colors.backgroundTertiary,
                                    contentColor = RadixTheme.colors.icon,
                                    disabledContentColor = RadixTheme.colors.backgroundTertiary,
                                    disabledContainerColor = RadixTheme.colors.icon
                                )
                            ) {
                                Icon(
                                    modifier = Modifier.size(16.dp),
                                    painter = painterResource(id = R.drawable.ic_copy),
                                    contentDescription = "copy"
                                )
                                Text(
                                    modifier = Modifier.padding(start = RadixTheme.dimensions.paddingXXSmall),
                                    text = stringResource(R.string.common_copy),
                                    style = RadixTheme.typography.body1Header
                                )
                            }
                        }

                        if (state.isRawManifestToggleInPreview) {
                            TransactionRawManifestToggle(
                                isToggleOn = state.isRawManifestVisible,
                                onRawManifestClick = onRawManifestToggle
                            )
                        }
                    }

                    if (state.showReceiptEdges) {
                        ReceiptEdge(color = RadixTheme.colors.background)
                    }
                }

                Column(modifier = Modifier.background(RadixTheme.colors.background)) {
                    if (state.showReceiptEdges) {
                        ReceiptEdge(color = RadixTheme.colors.backgroundSecondary)
                    }

                    if (!state.isPreAuthorization) {
                        PresentingProofsContent(
                            modifier = Modifier.padding(RadixTheme.dimensions.paddingDefault),
                            badges = state.previewType.badges.toPersistentList(),
                            onInfoClick = onInfoClick,
                            onClick = { badge ->
                                when (val resource = badge.resource) {
                                    is Resource.FungibleResource -> onTransferableFungibleClick(
                                        Transferable.FungibleType.Token(
                                            asset = Token(resource = resource),
                                            amount = BoundedAmount.Exact(amount = resource.ownedAmount.orZero()),
                                            isNewlyCreated = false
                                        )
                                    )

                                    is Resource.NonFungibleResource -> onTransferableNonFungibleItemClick(
                                        Transferable.NonFungibleType.NFTCollection(
                                            asset = NonFungibleCollection(resource),
                                            amount = NonFungibleAmount(certain = resource.items),
                                            isNewlyCreated = false
                                        ),
                                        resource.items.firstOrNull()
                                    )
                                }
                            }
                        )
                    }

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

                    state.expiration?.let { expiration ->
                        TransactionExpirationInfo(
                            modifier = Modifier.padding(RadixTheme.dimensions.paddingSmall),
                            expiration = expiration,
                            proposingDApp = state.proposingDApp ?: State.ProposingDApp.Unknown,
                            onInfoClick = onInfoClick
                        )
                    }

                    if (state.isSubmitVisible) {
                        SlideToSignButton(
                            modifier = Modifier
                                .padding(
                                    horizontal = if (state.isPreAuthorization) {
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
                                id = if (state.isPreAuthorization) {
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
        }
    }

    if (state.isSheetVisible) {
        DefaultModalSheetLayout(
            modifier = modifier.fillMaxSize(),
            sheetState = modalBottomSheetState,
            heightFraction = state.sheetState.sheetHeightPercent,
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
                    onFailedSigningRestart = onFailedSigningRestart,
                    onFailedSigningCancel = onFailedSigningCancel,
                    onInfoClick = onInfoClick
                )
            },
            showDragHandle = true,
            onDismissRequest = onBackClick
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
    onFailedSigningRestart: () -> Unit,
    onFailedSigningCancel: () -> Unit,
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

        is State.Sheet.SigningFailed -> {
            SigningFailedSheet(
                modifier = modifier,
                state = sheetState,
                onDismiss = onCloseBottomSheetClick,
                onRestartSigning = onFailedSigningRestart,
                onCancelTransaction = onFailedSigningCancel
            )
        }
        is State.Sheet.None -> {}
    }
}

@Preview
@UsesSampleValues
@Composable
private fun TransactionPreviewContentPreviewLight(
    @PreviewParameter(TransactionReviewPreviewProvider::class) state: State
) {
    RadixWalletPreviewTheme {
        TransactionReviewContent(
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
            onTransferableNonFungibleItemClick = { _, _ -> },
            onTransferableNonFungibleByAmountClick = { _, _ -> },
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
            onFailedSigningRestart = {},
            onFailedSigningCancel = {},
            onInfoClick = {}
        )
    }
}

@Preview
@UsesSampleValues
@Composable
private fun TransactionPreviewContentPreviewDark(
    @PreviewParameter(TransactionReviewPreviewProvider::class) state: State
) {
    RadixWalletPreviewTheme(enableDarkTheme = true) {
        TransactionReviewContent(
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
            onTransferableNonFungibleItemClick = { _, _ -> },
            onTransferableNonFungibleByAmountClick = { _, _ -> },
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
            onFailedSigningRestart = {},
            onFailedSigningCancel = {},
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
                proposingDApp = State.ProposingDApp.Some(
                    DApp(
                        dAppAddress = AccountAddress.sampleMainnet()
                    )
                ),
                previewType = PreviewType.Transaction(
                    from = listOf(
                        AccountWithTransferables(
                            account = InvolvedAccount.Owned(Account.sampleStokenet()),
                            transferables = listOf(
                                Transferable.FungibleType.Token(
                                    asset = Token(resource = Resource.FungibleResource.sampleMainnet()),
                                    amount = BoundedAmount.Exact("745".toDecimal192()),
                                    isNewlyCreated = false
                                )
                            )
                        )
                    ),
                    to = listOf(
                        AccountWithTransferables(
                            account = InvolvedAccount.Owned(Account.sampleMainnet()),
                            transferables = listOf(
                                Transferable.FungibleType.Token(
                                    asset = Token(resource = Resource.FungibleResource.sampleMainnet()),
                                    amount = BoundedAmount.Exact("745".toDecimal192()),
                                    isNewlyCreated = false
                                )
                            )
                        )
                    ),
                    involvedComponents = PreviewType.Transaction.InvolvedComponents.DApps(
                        components = linkedMapOf(
                            ManifestEncounteredComponentAddress.sampleMainnet() to DApp.sampleMainnet()
                        )
                    ),
                    badges = listOf(
                        Badge.sample(),
                        Badge.sample.other()
                    )
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
                isPreAuthorization = true,
                proposingDApp = State.ProposingDApp.Some(
                    DApp(
                        dAppAddress = AccountAddress.sampleMainnet()
                    )
                ),
                previewType = PreviewType.Transaction(
                    from = listOf(
                        AccountWithTransferables(
                            account = InvolvedAccount.Owned(Account.sampleStokenet()),
                            transferables = listOf(
                                Transferable.FungibleType.Token(
                                    asset = Token(resource = Resource.FungibleResource.sampleMainnet()),
                                    amount = BoundedAmount.Exact("745".toDecimal192()),
                                    isNewlyCreated = true
                                )
                            )
                        )
                    ),
                    to = listOf(
                        AccountWithTransferables(
                            account = InvolvedAccount.Owned(Account.sampleMainnet()),
                            transferables = listOf(
                                Transferable.FungibleType.Token(
                                    asset = Token(resource = Resource.FungibleResource.sampleMainnet()),
                                    amount = BoundedAmount.Exact("745".toDecimal192()),
                                    isNewlyCreated = true
                                )
                            )
                        )
                    ),
                    involvedComponents = PreviewType.Transaction.InvolvedComponents.DApps(
                        components = linkedMapOf(
                            ManifestEncounteredComponentAddress.sampleMainnet() to DApp.sampleMainnet()
                        ),
                        morePossibleDAppsPresent = true
                    ),
                    badges = listOf(Badge.sample())
                ),
                fees = null
            ),
            State(
                isLoading = false,
                previewType = PreviewType.DeleteAccount(
                    deletingAccount = Account.sampleMainnet(),
                    to = AccountWithTransferables(
                        account = InvolvedAccount.Owned(Account.sampleMainnet()),
                        transferables = listOf(
                            Transferable.FungibleType.Token(
                                asset = Token(resource = Resource.FungibleResource.sampleMainnet()),
                                amount = BoundedAmount.Exact("745".toDecimal192()),
                                isNewlyCreated = true
                            )
                        )
                    )
                ),
                fees = State.Fees(
                    isNetworkFeeLoading = false,
                    properties = State.Fees.Properties(),
                    transactionFees = TransactionFees(),
                    selectedFeePayerInput = null
                )
            ),
            State(
                isLoading = true,
                previewType = PreviewType.None
            )
        )
}
