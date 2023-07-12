@file:OptIn(ExperimentalMaterialApi::class, ExperimentalMaterialApi::class)

package com.babylon.wallet.android.presentation.transaction

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.ModalBottomSheetState
import androidx.compose.material.ModalBottomSheetValue
import androidx.compose.material.rememberModalBottomSheetState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
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
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.babylon.wallet.android.R
import com.babylon.wallet.android.data.transaction.TransactionVersion
import com.babylon.wallet.android.designsystem.composable.RadixTextButton
import com.babylon.wallet.android.designsystem.theme.RadixTheme
import com.babylon.wallet.android.designsystem.theme.RadixWalletTheme
import com.babylon.wallet.android.domain.model.DAppWithMetadataAndAssociatedResources
import com.babylon.wallet.android.domain.model.MessageFromDataChannel
import com.babylon.wallet.android.domain.model.TransactionManifestData
import com.babylon.wallet.android.presentation.common.FullscreenCircularProgressContent
import com.babylon.wallet.android.presentation.dapp.authorized.account.AccountItemUiModel
import com.babylon.wallet.android.presentation.settings.dappdetail.DAppDetailsSheetContent
import com.babylon.wallet.android.presentation.transaction.composables.FeePayerSelectionSheet
import com.babylon.wallet.android.presentation.transaction.composables.GuaranteesSheet
import com.babylon.wallet.android.presentation.transaction.composables.TransactionPreviewHeader
import com.babylon.wallet.android.presentation.transaction.composables.TransactionPreviewTypeContent
import com.babylon.wallet.android.presentation.ui.composables.DefaultModalSheetLayout
import com.babylon.wallet.android.presentation.ui.composables.NotSecureAlertDialog
import com.babylon.wallet.android.presentation.ui.composables.RadixSnackbarHost
import com.babylon.wallet.android.presentation.ui.composables.SnackbarUIMessage
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import rdx.works.profile.data.model.apppreferences.Radix

private const val PAYER_DIALOG_CLOSE_DELAY = 300L

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun TransactionApprovalScreen(
    modifier: Modifier = Modifier,
    viewModel: TransactionApprovalViewModel2,
    onDismiss: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val state by viewModel.state.collectAsStateWithLifecycle()

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
                TransactionApprovalViewModel2.Event.Dismiss -> {
                    onDismiss()
                }

                TransactionApprovalViewModel2.Event.SelectFeePayer -> {
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
@OptIn(ExperimentalMaterialApi::class, ExperimentalMaterial3Api::class)
private fun TransactionPreviewContent(
    modifier: Modifier = Modifier,
    onBackClick: () -> Unit,
    state: TransactionApprovalViewModel2.State,
    onApproveTransaction: () -> Unit,
    onMessageShown: () -> Unit,
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

    val snackBarHostState = remember { SnackbarHostState() }
    SnackbarUIMessage(
        message = state.error,
        snackbarHostState = snackBarHostState,
        onMessageShown = onMessageShown
    )

    DefaultModalSheetLayout(
        modifier = modifier.fillMaxSize(),
        sheetState = modalBottomSheetState,
        sheetContent = {
//            BottomSheetContent(
//                bottomSheetViewMode = state.bottomSheetViewMode,
//                feePayerCandidates = state.feePayerCandidates,
//                onPayerSelected = onPayerSelected,
//                onPayerConfirmed = onPayerConfirmed,
//                guaranteesAccounts = state.guaranteesAccounts,
//                onGuaranteesCloseClick = {
//                    scope.launch {
//                        modalBottomSheetState.hide()
//                    }
//                    onGuaranteesCloseClick()
//                },
//                onGuaranteesApplyClick = {
//                    scope.launch {
//                        modalBottomSheetState.hide()
//                    }
//                    onGuaranteesApplyClick()
//                },
//                onGuaranteeValueChanged = {
//                    onGuaranteeValueChanged(it)
//                },
//                onCloseFeePayerSheet = {
//                    scope.launch {
//                        modalBottomSheetState.hide()
//                    }
//                },
//                onCloseDAppSheet = {
//                    scope.launch {
//                        modalBottomSheetState.hide()
//                    }
//                }
//            )
        }
    ) {
        val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()
        Scaffold(
            modifier = Modifier
                .fillMaxSize()
                .navigationBarsPadding()
                .nestedScroll(scrollBehavior.nestedScrollConnection),
            topBar = {
                TransactionPreviewHeader(
                    onBackClick = onBackClick,
                    onRawManifestClick = { showRawManifest = !showRawManifest },
                    onBackEnabled = !state.isSigning,
                    scrollBehavior = scrollBehavior
                )
            },
            snackbarHost = {
                RadixSnackbarHost(
                    modifier = Modifier.padding(horizontal = RadixTheme.dimensions.paddingDefault),
                    hostState = snackBarHostState
                )
            },
            containerColor = RadixTheme.colors.defaultBackground
        ) { padding ->
            Box(modifier = Modifier.padding(padding)) {
                if (state.isLoading) {
                    FullscreenCircularProgressContent()
                } else {
                    when (state.previewType) {
                        is PreviewType.NonConforming -> TODO()
                        is PreviewType.Transaction -> {
                            TransactionPreviewTypeContent(
                                modifier = Modifier.verticalScroll(rememberScrollState()),
                                state = state,
                                preview = state.previewType,
                                onApproveTransaction = onApproveTransaction
                            )
                        }
                    }
                }
            }
        }
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
        TransactionPreviewContent(
            onBackClick = {},
            state = TransactionApprovalViewModel2.State(
                request = MessageFromDataChannel.IncomingRequest.TransactionRequest(
                    dappId = "",
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
                isDeviceSecure = true,
                previewType = PreviewType.NonConforming
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
