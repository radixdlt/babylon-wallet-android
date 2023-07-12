@file:OptIn(ExperimentalMaterialApi::class, ExperimentalMaterialApi::class)

package com.babylon.wallet.android.presentation.transaction

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.ModalBottomSheetState
import androidx.compose.material.ModalBottomSheetValue
import androidx.compose.material.rememberModalBottomSheetState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.babylon.wallet.android.R
import com.babylon.wallet.android.data.transaction.TransactionVersion
import com.babylon.wallet.android.designsystem.composable.RadixPrimaryButton
import com.babylon.wallet.android.designsystem.theme.RadixTheme
import com.babylon.wallet.android.designsystem.theme.RadixWalletTheme
import com.babylon.wallet.android.domain.model.DAppWithMetadataAndAssociatedResources
import com.babylon.wallet.android.domain.model.MessageFromDataChannel
import com.babylon.wallet.android.domain.model.TransactionManifestData
import com.babylon.wallet.android.presentation.common.FullscreenCircularProgressContent
import com.babylon.wallet.android.presentation.transaction.composables.FeePayerSelectionSheet
import com.babylon.wallet.android.presentation.transaction.composables.RawManifestView
import com.babylon.wallet.android.presentation.transaction.composables.TransactionPreviewHeader
import com.babylon.wallet.android.presentation.transaction.composables.TransactionPreviewTypeContent
import com.babylon.wallet.android.presentation.ui.composables.DefaultModalSheetLayout
import com.babylon.wallet.android.presentation.ui.composables.NotSecureAlertDialog
import com.babylon.wallet.android.presentation.ui.composables.RadixSnackbarHost
import com.babylon.wallet.android.presentation.ui.composables.SnackbarUIMessage
import com.babylon.wallet.android.utils.biometricAuthenticate
import com.babylon.wallet.android.utils.findFragmentActivity
import kotlinx.coroutines.launch
import rdx.works.profile.data.model.apppreferences.Radix
import rdx.works.profile.data.model.pernetwork.Network

@Composable
fun TransactionApprovalScreen(
    modifier: Modifier = Modifier,
    viewModel: TransactionApprovalViewModel2,
    onDismiss: () -> Unit
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    TransactionPreviewContent(
        onBackClick = viewModel::onBackClick,
        state = state,
        onApproveTransaction = viewModel::approveTransaction,
        onRawManifestToggle = viewModel::onRawManifestToggle,
        onMessageShown = viewModel::onMessageShown,
        modifier = modifier,
        onGuaranteesApplyClick = viewModel::onGuaranteesApplyClick,
        onGuaranteesCloseClick = viewModel::onGuaranteesCloseClick,
        promptForGuaranteesClick = viewModel::promptForGuaranteesClick,
        onDAppClick = viewModel::onDAppClick,
        onGuaranteeValueChanged = viewModel::onGuaranteeValueChanged,
        onPayerSelected = viewModel::onPayerSelected,
        onPayerConfirmed =  viewModel::onPayerConfirmed,
        resetBottomSheetMode = viewModel::resetBottomSheetMode
    )
    LaunchedEffect(Unit) {
        viewModel.oneOffEvent.collect { event ->
            when (event) {
                TransactionApprovalViewModel2.Event.Dismiss -> {
                    onDismiss()
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
    onRawManifestToggle: () -> Unit,
    onMessageShown: () -> Unit,
    onGuaranteesApplyClick: () -> Unit,
    onGuaranteesCloseClick: () -> Unit,
    promptForGuaranteesClick: () -> Unit,
    onDAppClick: (DAppWithMetadataAndAssociatedResources) -> Unit,
    onGuaranteeValueChanged: (Pair<String, GuaranteesAccountItemUiModel>) -> Unit,
    onPayerSelected: (Network.Account) -> Unit,
    onPayerConfirmed: () -> Unit,
    resetBottomSheetMode: () -> Unit
) {
    val modalBottomSheetState = rememberModalBottomSheetState(
        initialValue = ModalBottomSheetValue.Hidden,
        skipHalfExpanded = true
    )
    val snackBarHostState = remember { SnackbarHostState() }

    SnackbarUIMessage(
        message = state.error,
        snackbarHostState = snackBarHostState,
        onMessageShown = onMessageShown
    )

    BackHandler(onBack = onBackClick)

    SyncSheetState(
        bottomSheetState = modalBottomSheetState,
        isSheetVisible = state.isSheetVisible,
        onSheetClosed = {
            if (state.isSheetVisible) {
                onBackClick()
            }
        }
    )

    DefaultModalSheetLayout(
        modifier = modifier
            .navigationBarsPadding()
            .fillMaxSize(),
        sheetState = modalBottomSheetState,
        sheetContent = {
            BottomSheetContent(
                sheetState = state.sheetState,
                onPayerSelected = onPayerSelected,
                onPayerConfirmed = onPayerConfirmed,
                onGuaranteesCloseClick = {
                    onGuaranteesCloseClick()
                },
                onGuaranteesApplyClick = {
                    onGuaranteesApplyClick()
                },
                onGuaranteeValueChanged = {
                    onGuaranteeValueChanged(it)
                },
                onCloseFeePayerSheet = onBackClick,
                onCloseDAppSheet = onBackClick,
            )
        }
    ) {
        val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()
        Scaffold(
            modifier = Modifier
                .fillMaxSize()
                .nestedScroll(scrollBehavior.nestedScrollConnection),
            topBar = {
                TransactionPreviewHeader(
                    onBackClick = onBackClick,
                    onRawManifestClick = onRawManifestToggle,
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
            contentWindowInsets = WindowInsets.ime,
            containerColor = RadixTheme.colors.gray5
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
                            RawManifestView(
                                modifier = Modifier.padding(RadixTheme.dimensions.paddingDefault),
                                manifest = state.rawManifest
                            )

                            ApproveButton(
                                state = state,
                                onApproveTransaction = onApproveTransaction
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
                            when (state.previewType) {
                                is PreviewType.NonConforming -> {}
                                is PreviewType.Transaction -> {
                                    TransactionPreviewTypeContent(
                                        state = state,
                                        preview = state.previewType
                                    )
                                }
                            }

                            ApproveButton(
                                state = state,
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
private fun ApproveButton(
    state: TransactionApprovalViewModel2.State,
    onApproveTransaction: () -> Unit
) {
    var showNotSecuredDialog by remember { mutableStateOf(false) }
    val context = LocalContext.current
    RadixPrimaryButton(
        modifier = Modifier
            .fillMaxWidth()
            .padding(RadixTheme.dimensions.paddingDefault),
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
        enabled = !state.isSigning /*&& state.canApprove && state.bottomSheetViewMode != BottomSheetMode.FeePayerSelection*/,
        icon = {
            Icon(
                painter = painterResource(id = com.babylon.wallet.android.designsystem.R.drawable.ic_lock),
                contentDescription = ""
            )
        }
    )

    if (showNotSecuredDialog) {
        NotSecureAlertDialog(
            finish = { accepted ->
                showNotSecuredDialog = false
                if (accepted) {
                    onApproveTransaction()
                }
            }
        )
    }
}

@Composable
private fun BottomSheetContent(
    sheetState: TransactionApprovalViewModel2.State.Sheet,
    onPayerSelected: (Network.Account) -> Unit,
    onPayerConfirmed: () -> Unit,
    onGuaranteesCloseClick: () -> Unit,
    onGuaranteesApplyClick: () -> Unit,
    onGuaranteeValueChanged: (Pair<String, GuaranteesAccountItemUiModel>) -> Unit,
    onCloseFeePayerSheet: () -> Unit,
    onCloseDAppSheet: () -> Unit
) {
    when (sheetState) {
//        BottomSheetMode.Guarantees -> {
//            GuaranteesSheet(
//                modifier = Modifier.fillMaxWidth(),
//                guaranteesAccounts = guaranteesAccounts,
//                onClose = onGuaranteesCloseClick,
//                onApplyClick = onGuaranteesApplyClick,
//                onGuaranteeValueChanged = onGuaranteeValueChanged
//            )
//        }

//        is BottomSheetMode.DApp -> {
//            DAppDetailsSheetContent(
//                onBackClick = onCloseDAppSheet,
//                dappName = bottomSheetViewMode.dApp.dAppWithMetadata.name.orEmpty(),
//                dappWithMetadata = bottomSheetViewMode.dApp.dAppWithMetadata,
//                associatedFungibleTokens = bottomSheetViewMode.dApp.fungibleResources.toPersistentList(),
//                associatedNonFungibleTokens = bottomSheetViewMode.dApp.nonFungibleResources.toPersistentList()
//            )
//        }

        is TransactionApprovalViewModel2.State.Sheet.FeePayerChooser -> {
            FeePayerSelectionSheet(
                modifier = Modifier.fillMaxWidth(),
                sheet = sheetState,
                onClose = onCloseFeePayerSheet,
                onPayerSelected = onPayerSelected,
                onPayerConfirmed = onPayerConfirmed
            )
        }
        is TransactionApprovalViewModel2.State.Sheet.None -> {}
    }
}

@Composable
private fun SyncSheetState(
    bottomSheetState: ModalBottomSheetState,
    isSheetVisible: Boolean,
    onSheetClosed: () -> Unit,
) {
    val scope = rememberCoroutineScope()

    LaunchedEffect(isSheetVisible) {
        if (isSheetVisible) {
            scope.launch { bottomSheetState.show() }
        } else {
            scope.launch { bottomSheetState.hide() }
        }
    }

    LaunchedEffect(bottomSheetState.isVisible) {
        if (!bottomSheetState.isVisible) {
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
            onRawManifestToggle = {},
            onMessageShown = {},
            onGuaranteesApplyClick = {},
            onGuaranteesCloseClick = {},
            promptForGuaranteesClick = {},
            onDAppClick = {},
            onGuaranteeValueChanged = {},
            onPayerSelected = {},
            onPayerConfirmed = {},
            resetBottomSheetMode = {}
        )
    }
}
