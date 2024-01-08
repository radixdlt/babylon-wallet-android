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
import androidx.compose.foundation.layout.fillMaxWidth
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
import com.babylon.wallet.android.data.transaction.TransactionVersion
import com.babylon.wallet.android.designsystem.theme.RadixTheme
import com.babylon.wallet.android.designsystem.theme.RadixWalletTheme
import com.babylon.wallet.android.domain.model.DAppWithResources
import com.babylon.wallet.android.domain.model.MessageFromDataChannel
import com.babylon.wallet.android.domain.model.TransactionManifestData
import com.babylon.wallet.android.domain.model.resources.Resource
import com.babylon.wallet.android.domain.userFriendlyMessage
import com.babylon.wallet.android.presentation.common.FullscreenCircularProgressContent
import com.babylon.wallet.android.presentation.settings.authorizeddapps.dappdetail.DAppDetailsSheetContent
import com.babylon.wallet.android.presentation.settings.authorizeddapps.dappdetail.UnknownDAppComponentsSheetContent
import com.babylon.wallet.android.presentation.status.signing.FactorSourceInteractionBottomDialog
import com.babylon.wallet.android.presentation.transaction.TransactionReviewViewModel.State
import com.babylon.wallet.android.presentation.transaction.composables.AccountDepositSettingsTypeContent
import com.babylon.wallet.android.presentation.transaction.composables.FeesSheet
import com.babylon.wallet.android.presentation.transaction.composables.GuaranteesSheet
import com.babylon.wallet.android.presentation.transaction.composables.NetworkFeeContent
import com.babylon.wallet.android.presentation.transaction.composables.PresentingProofsContent
import com.babylon.wallet.android.presentation.transaction.composables.RawManifestView
import com.babylon.wallet.android.presentation.transaction.composables.TransactionPreviewHeader
import com.babylon.wallet.android.presentation.transaction.composables.TransactionPreviewTypeContent
import com.babylon.wallet.android.presentation.transaction.fees.TransactionFees
import com.babylon.wallet.android.presentation.ui.composables.BasicPromptAlertDialog
import com.babylon.wallet.android.presentation.ui.composables.DefaultModalSheetLayout
import com.babylon.wallet.android.presentation.ui.composables.RadixSnackbarHost
import com.babylon.wallet.android.presentation.ui.composables.ReceiptEdge
import com.babylon.wallet.android.presentation.ui.composables.SlideToSignButton
import com.babylon.wallet.android.presentation.ui.composables.SnackbarUIMessage
import com.babylon.wallet.android.utils.biometricAuthenticateSuspend
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.launch
import rdx.works.profile.data.model.apppreferences.Radix
import rdx.works.profile.data.model.pernetwork.Network

@Composable
fun TransactionReviewScreen(
    modifier: Modifier = Modifier,
    viewModel: TransactionReviewViewModel,
    onDismiss: () -> Unit,
    onFungibleClick: (Resource.FungibleResource, Boolean) -> Unit,
    onNonFungibleClick: (Resource.NonFungibleResource, Resource.NonFungibleResource.Item, Boolean) -> Unit
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        viewModel.oneOffEvent.collect {
            when (it) {
                is TransactionReviewViewModel.Event.OnFungibleClick -> onFungibleClick(it.resource, it.isNewlyCreated)
                is TransactionReviewViewModel.Event.OnNonFungibleClick -> onNonFungibleClick(it.resource, it.item, it.isNewlyCreated)
            }
        }
    }

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
        onGuaranteesCloseClick = viewModel::onGuaranteesCloseClick,
        onGuaranteeValueChanged = viewModel::onGuaranteeValueChange,
        onGuaranteeValueIncreased = viewModel::onGuaranteeValueIncreased,
        onGuaranteeValueDecreased = viewModel::onGuaranteeValueDecreased,
        onDAppClick = viewModel::onDAppClick,
        onUnknownDAppsClick = viewModel::onUnknownDAppsClick,
        onFungibleResourceClick = viewModel::onFungibleResourceClick,
        onNonFungibleResourceClick = viewModel::onNonFungibleResourceClick,
        onChangeFeePayerClick = viewModel::onChangeFeePayerClick,
        onSelectFeePayerClick = viewModel::onSelectFeePayerClick,
        onPayerSelected = viewModel::onPayerSelected,
        onFeePaddingAmountChanged = viewModel::onFeePaddingAmountChanged,
        onTipPercentageChanged = viewModel::onTipPercentageChanged,
        onViewDefaultModeClick = viewModel::onViewDefaultModeClick,
        onViewAdvancedModeClick = viewModel::onViewAdvancedModeClick,
        dismissTransactionErrorDialog = viewModel::dismissTransactionErrorDialog
    )

    state.interactionState?.let {
        when (it) {
            is InteractionState.Ledger.Error -> {
                BasicPromptAlertDialog(
                    finish = { viewModel.onCancelSigningClick() },
                    text = {
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
    onGuaranteesCloseClick: () -> Unit,
    onGuaranteeValueChanged: (AccountWithPredictedGuarantee, String) -> Unit,
    onGuaranteeValueIncreased: (AccountWithPredictedGuarantee) -> Unit,
    onGuaranteeValueDecreased: (AccountWithPredictedGuarantee) -> Unit,
    onDAppClick: (DAppWithResources) -> Unit,
    onUnknownDAppsClick: (ImmutableList<String>) -> Unit,
    onFungibleResourceClick: (Resource.FungibleResource, Boolean) -> Unit,
    onNonFungibleResourceClick: (Resource.NonFungibleResource, Resource.NonFungibleResource.Item, Boolean) -> Unit,
    onChangeFeePayerClick: () -> Unit,
    onSelectFeePayerClick: () -> Unit,
    onPayerSelected: (Network.Account) -> Unit,
    onFeePaddingAmountChanged: (String) -> Unit,
    onTipPercentageChanged: (String) -> Unit,
    onViewDefaultModeClick: () -> Unit,
    onViewAdvancedModeClick: () -> Unit,
    dismissTransactionErrorDialog: () -> Unit
) {
    val modalBottomSheetState = rememberModalBottomSheetState(
        skipPartiallyExpanded = true
    )
    val snackBarHostState = remember { SnackbarHostState() }

    state.error?.let { transactionError ->
        if (transactionError.isPreviewedInDialog) {
            BasicPromptAlertDialog(
                finish = {
                    dismissTransactionErrorDialog()
                },
                title = transactionError.getTitle(),
                text = transactionError.getMessage(),
                confirmText = stringResource(id = R.string.common_ok),
                dismissText = null
            )
        } else {
            SnackbarUIMessage(
                message = transactionError,
                snackbarHostState = snackBarHostState,
                onMessageShown = onMessageShown
            )
        }
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
        Box(modifier = Modifier.padding(padding)) {
            if (state.isLoading) {
                FullscreenCircularProgressContent()
            } else {
                AnimatedVisibility(
                    visible = state.isRawManifestVisible,
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    Column(
                        modifier = Modifier.verticalScroll(rememberScrollState())
                    ) {
                        ReceiptEdge(modifier = Modifier.fillMaxWidth(), color = RadixTheme.colors.gray5, topEdge = true)
                        RawManifestView(
                            modifier = Modifier
                                .background(color = RadixTheme.colors.gray5)
                                .padding(RadixTheme.dimensions.paddingDefault),
                            manifest = state.rawManifest
                        )
                        ReceiptEdge(modifier = Modifier.fillMaxWidth(), color = RadixTheme.colors.gray5)
                        NetworkFeeContent(
                            fees = state.transactionFees,
                            noFeePayerSelected = state.noFeePayerSelected,
                            insufficientBalanceToPayTheFee = state.isBalanceInsufficientToPayTheFee,
                            isNetworkFeeLoading = state.isNetworkFeeLoading,
                            onCustomizeClick = onCustomizeClick
                        )
                        SlideToSignButton(
                            modifier = Modifier.padding(
                                horizontal = RadixTheme.dimensions.paddingXLarge,
                                vertical = RadixTheme.dimensions.paddingDefault
                            ),
                            enabled = state.isSubmitEnabled,
                            isSubmitting = state.isSubmitting,
                            onSwipeComplete = onApproveTransaction
                        )
                    }
                }

                AnimatedVisibility(
                    visible = !state.isRawManifestVisible,
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    Column(
                        modifier = Modifier.verticalScroll(rememberScrollState())
                    ) {
                        ReceiptEdge(modifier = Modifier.fillMaxWidth(), color = RadixTheme.colors.gray5, topEdge = true)
                        when (val preview = state.previewType) {
                            is PreviewType.None -> {}
                            is PreviewType.UnacceptableManifest -> {
                                return@AnimatedVisibility
                            }

                            is PreviewType.NonConforming -> {}
                            is PreviewType.Transfer -> {
                                TransactionPreviewTypeContent(
                                    modifier = Modifier.background(RadixTheme.colors.gray5),
                                    state = state,
                                    preview = preview,
                                    onPromptForGuarantees = promptForGuarantees,
                                    onDappClick = onDAppClick,
                                    onUnknownDAppsClick = onUnknownDAppsClick,
                                    onFungibleResourceClick = onFungibleResourceClick,
                                    onNonFungibleResourceClick = onNonFungibleResourceClick
                                )
                                ReceiptEdge(modifier = Modifier.fillMaxWidth(), color = RadixTheme.colors.gray5)
                                PresentingProofsContent(
                                    badges = preview.badges.toPersistentList()
                                )
                            }

                            is PreviewType.AccountsDepositSettings -> {
                                AccountDepositSettingsTypeContent(
                                    modifier = Modifier.background(RadixTheme.colors.gray5),
                                    preview = preview
                                )
                                ReceiptEdge(modifier = Modifier.fillMaxWidth(), color = RadixTheme.colors.gray5)
                            }
                        }
                        NetworkFeeContent(
                            fees = state.transactionFees,
                            noFeePayerSelected = state.noFeePayerSelected,
                            insufficientBalanceToPayTheFee = state.isBalanceInsufficientToPayTheFee,
                            isNetworkFeeLoading = state.isNetworkFeeLoading,
                            onCustomizeClick = onCustomizeClick
                        )
                        SlideToSignButton(
                            modifier = Modifier.padding(
                                horizontal = RadixTheme.dimensions.paddingXLarge,
                                vertical = RadixTheme.dimensions.paddingDefault
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
            sheetContent = {
                BottomSheetContent(
                    modifier = Modifier.navigationBarsPadding(),
                    sheetState = state.sheetState,
                    transactionFees = state.transactionFees,
                    insufficientBalanceToPayTheFee = state.isBalanceInsufficientToPayTheFee,
                    onGuaranteesCloseClick = onGuaranteesCloseClick,
                    onGuaranteesApplyClick = onGuaranteesApplyClick,
                    onGuaranteeValueChanged = onGuaranteeValueChanged,
                    onGuaranteeValueIncreased = onGuaranteeValueIncreased,
                    onGuaranteeValueDecreased = onGuaranteeValueDecreased,
                    onCloseDAppSheet = onBackClick,
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
    onGuaranteesCloseClick: () -> Unit,
    onGuaranteesApplyClick: () -> Unit,
    onGuaranteeValueChanged: (AccountWithPredictedGuarantee, String) -> Unit,
    onGuaranteeValueIncreased: (AccountWithPredictedGuarantee) -> Unit,
    onGuaranteeValueDecreased: (AccountWithPredictedGuarantee) -> Unit,
    onCloseDAppSheet: () -> Unit,
    onChangeFeePayerClick: () -> Unit,
    onSelectFeePayerClick: () -> Unit,
    onPayerSelected: (Network.Account) -> Unit,
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
                onClose = onGuaranteesCloseClick,
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
                onClose = onGuaranteesCloseClick,
                onChangeFeePayerClick = onChangeFeePayerClick,
                onSelectFeePayerClick = onSelectFeePayerClick,
                onPayerSelected = onPayerSelected,
                onFeePaddingAmountChanged = onFeePaddingAmountChanged,
                onTipPercentageChanged = onTipPercentageChanged,
                onViewDefaultModeClick = onViewDefaultModeClick,
                onViewAdvancedModeClick = onViewAdvancedModeClick
            )
        }

        is State.Sheet.Dapp -> {
            DAppDetailsSheetContent(
                modifier = modifier,
                onBackClick = onCloseDAppSheet,
                dApp = sheetState.dApp
            )
        }

        is State.Sheet.UnknownDAppComponents -> {
            UnknownDAppComponentsSheetContent(
                modifier = modifier,
                onBackClick = onCloseDAppSheet,
                unknownDAppComponents = sheetState.unknownComponentAddresses
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
                        version = TransactionVersion.Default.value,
                        networkId = Radix.Gateway.default.network.id,
                        message = "Hello"
                    ),
                    requestMetadata = MessageFromDataChannel.IncomingRequest.RequestMetadata.internal(Radix.Gateway.default.network.id)
                ),
                isLoading = false,
                isNetworkFeeLoading = false,
                previewType = PreviewType.NonConforming
            ),
            onApproveTransaction = {},
            onRawManifestToggle = {},
            onMessageShown = {},
            onGuaranteesApplyClick = {},
            onGuaranteesCloseClick = {},
            promptForGuarantees = {},
            onCustomizeClick = {},
            onDAppClick = {},
            onUnknownDAppsClick = {},
            onFungibleResourceClick = { _, _ -> },
            onNonFungibleResourceClick = { _, _, _ -> },
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
            dismissTransactionErrorDialog = {}
        )
    }
}
