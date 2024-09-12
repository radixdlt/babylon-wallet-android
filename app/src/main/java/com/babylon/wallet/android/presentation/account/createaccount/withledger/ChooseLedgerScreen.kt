package com.babylon.wallet.android.presentation.account.createaccount.withledger

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.babylon.wallet.android.R
import com.babylon.wallet.android.designsystem.theme.RadixTheme
import com.babylon.wallet.android.designsystem.theme.RadixWalletTheme
import com.babylon.wallet.android.domain.model.Selectable
import com.babylon.wallet.android.domain.model.toProfileLedgerDeviceModel
import com.babylon.wallet.android.presentation.common.UiMessage
import com.babylon.wallet.android.presentation.dialogs.info.GlossaryItem
import com.babylon.wallet.android.presentation.settings.linkedconnectors.AddLinkConnectorViewModel
import com.babylon.wallet.android.presentation.settings.securitycenter.ledgerhardwarewallets.AddLedgerDeviceViewModel
import com.babylon.wallet.android.presentation.settings.securitycenter.ledgerhardwarewallets.ShowLinkConnectorPromptState
import com.babylon.wallet.android.presentation.ui.composables.AddLedgerDeviceScreen
import com.babylon.wallet.android.presentation.ui.composables.AddLinkConnectorScreen
import com.babylon.wallet.android.presentation.ui.composables.BackIconType
import com.babylon.wallet.android.presentation.ui.composables.BasicPromptAlertDialog
import com.babylon.wallet.android.presentation.ui.composables.ChooseLedgerDeviceSection
import com.babylon.wallet.android.presentation.ui.composables.LinkConnectorScreen
import com.babylon.wallet.android.presentation.ui.composables.RadixBottomBar
import com.babylon.wallet.android.presentation.ui.composables.RadixCenteredTopAppBar
import com.babylon.wallet.android.presentation.ui.composables.RadixSnackbarHost
import com.babylon.wallet.android.presentation.ui.composables.SnackbarUIMessage
import com.babylon.wallet.android.presentation.ui.composables.statusBarsAndBanner
import com.radixdlt.sargon.FactorSource
import com.radixdlt.sargon.FactorSourceId
import com.radixdlt.sargon.annotation.UsesSampleValues
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.launch
import rdx.works.core.sargon.sample

@Composable
fun ChooseLedgerScreen(
    modifier: Modifier = Modifier,
    viewModel: ChooseLedgerViewModel,
    addLedgerDeviceViewModel: AddLedgerDeviceViewModel,
    addLinkConnectorViewModel: AddLinkConnectorViewModel,
    onInfoClick: (GlossaryItem) -> Unit,
    goBackToCreateAccount: () -> Unit,
    onStartRecovery: (FactorSourceId.Hash, Boolean) -> Unit,
    onBackClick: () -> Unit
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val addLedgerDeviceState by addLedgerDeviceViewModel.state.collectAsStateWithLifecycle()
    val addLinkConnectorState by addLinkConnectorViewModel.state.collectAsStateWithLifecycle()
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        viewModel.oneOffEvent.collect { event ->
            when (event) {
                is ChooseLedgerEvent.LedgerSelected -> goBackToCreateAccount()
                is ChooseLedgerEvent.RecoverAccounts -> onStartRecovery(event.factorSource, event.isOlympia)
            }
        }
    }

    LaunchedEffect(Unit) {
        addLinkConnectorViewModel.oneOffEvent.collect { event ->
            when (event) {
                is AddLinkConnectorViewModel.Event.Close -> {
                    if (event.isLinkedConnectorAdded) {
                        viewModel.onNewLinkedConnectorAdded()
                    } else {
                        viewModel.onCloseClick()
                    }
                }
            }
        }
    }

    val promptState = state.showLinkConnectorPromptState
    if (promptState is ShowLinkConnectorPromptState.Show) {
        BasicPromptAlertDialog(
            finish = {
                viewModel.dismissConnectorPrompt(it)
            },
            title = {
                Text(
                    text = stringResource(id = R.string.ledgerHardwareDevices_linkConnectorAlert_title),
                    style = RadixTheme.typography.body1Header,
                    color = RadixTheme.colors.gray1
                )
            },
            message = {
                Text(
                    text = stringResource(id = R.string.ledgerHardwareDevices_linkConnectorAlert_message),
                    style = RadixTheme.typography.body2Regular,
                    color = RadixTheme.colors.gray1
                )
            },
            confirmText = stringResource(id = R.string.ledgerHardwareDevices_linkConnectorAlert_continue)
        )
    }
    when (state.showContent) {
        ChooseLedgerUiState.ShowContent.ChooseLedger -> {
            ChooseLedgerDeviceContent(
                modifier = modifier,
                onBackClick = onBackClick,
                ledgerDevices = state.ledgerDevices,
                onLedgerDeviceSelected = viewModel::onLedgerDeviceSelected,
                onAddLedgerDeviceClick = viewModel::onAddLedgerDeviceClick,
                onUseLedgerContinueClick = viewModel::onUseLedgerContinueClick,
                isAddingNewLinkConnectorInProgress = addLinkConnectorState.isAddingNewLinkConnectorInProgress,
                uiMessage = state.uiMessage,
                onMessageShown = viewModel::onMessageShown
            )
        }

        is ChooseLedgerUiState.ShowContent.LinkNewConnector -> {
            LinkConnectorScreen(
                modifier = Modifier.fillMaxSize(),
                onLinkConnectorClick = {
                    viewModel.onLinkConnectorClick()
                },
                onCloseClick = viewModel::onCloseClick
            )
        }

        is ChooseLedgerUiState.ShowContent.AddLinkConnector -> {
            AddLinkConnectorScreen(
                modifier = Modifier,
                state = addLinkConnectorState,
                onQrCodeScanned = addLinkConnectorViewModel::onQrCodeScanned,
                onQrCodeScanFailure = addLinkConnectorViewModel::onQrCodeScanFailure,
                onConnectorDisplayNameChanged = addLinkConnectorViewModel::onConnectorDisplayNameChanged,
                onInfoClick = onInfoClick,
                onContinueClick = addLinkConnectorViewModel::onContinueClick,
                onCloseClick = addLinkConnectorViewModel::onCloseClick,
                onErrorDismiss = addLinkConnectorViewModel::onErrorDismiss
            )
        }

        ChooseLedgerUiState.ShowContent.AddLedger -> {
            AddLedgerDeviceScreen(
                modifier = Modifier,
                showContent = addLedgerDeviceState.showContent,
                deviceModel = addLedgerDeviceState.newConnectedLedgerDevice?.model?.toProfileLedgerDeviceModel(),
                uiMessage = addLedgerDeviceState.uiMessage,
                onSendAddLedgerRequestClick = addLedgerDeviceViewModel::onSendAddLedgerRequestClick,
                onConfirmLedgerNameClick = {
                    coroutineScope.launch {
                        addLedgerDeviceViewModel.onConfirmLedgerNameClick(it)
                        viewModel.onCloseClick()
                    }
                },
                backIconType = BackIconType.Back,
                onClose = viewModel::onCloseClick,
                waitingForLedgerResponse = false,
                onBackClick = viewModel::onCloseClick,
                onMessageShown = addLedgerDeviceViewModel::onMessageShown,
                isAddingLedgerDeviceInProgress = addLedgerDeviceState.isAddingLedgerDeviceInProgress,
                isAddingNewLinkConnectorInProgress = addLinkConnectorState.isAddingNewLinkConnectorInProgress
            )
        }
    }
}

@Composable
private fun ChooseLedgerDeviceContent(
    modifier: Modifier,
    ledgerDevices: ImmutableList<Selectable<FactorSource.Ledger>>,
    onLedgerDeviceSelected: (FactorSource.Ledger) -> Unit,
    onUseLedgerContinueClick: () -> Unit,
    onAddLedgerDeviceClick: () -> Unit,
    onBackClick: () -> Unit,
    isAddingNewLinkConnectorInProgress: Boolean,
    uiMessage: UiMessage? = null,
    onMessageShown: () -> Unit
) {
    BackHandler { onBackClick() }

    val snackBarHostState = remember { SnackbarHostState() }
    SnackbarUIMessage(
        message = uiMessage,
        snackbarHostState = snackBarHostState,
        onMessageShown = onMessageShown
    )

    Scaffold(
        modifier = modifier,
        topBar = {
            RadixCenteredTopAppBar(
                title = stringResource(id = R.string.empty),
                onBackClick = onBackClick,
                backIconType = BackIconType.Close,
                windowInsets = WindowInsets.statusBarsAndBanner
            )
        },
        bottomBar = {
            RadixBottomBar(
                onClick = onUseLedgerContinueClick,
                text = stringResource(id = R.string.ledgerHardwareDevices_continueWithLedger),
                enabled = ledgerDevices.any { it.selected },
                isLoading = isAddingNewLinkConnectorInProgress
            )
        },
        snackbarHost = {
            RadixSnackbarHost(
                hostState = snackBarHostState,
                modifier = Modifier.padding(RadixTheme.dimensions.paddingDefault)
            )
        },
        containerColor = RadixTheme.colors.defaultBackground
    ) { padding ->
        ChooseLedgerDeviceSection(
            modifier = Modifier
                .fillMaxSize()
                .padding(
                    horizontal = RadixTheme.dimensions.paddingLarge,
                    vertical = RadixTheme.dimensions.paddingDefault
                )
                .padding(padding),
            ledgerDevices = ledgerDevices,
            onAddLedgerDeviceClick = onAddLedgerDeviceClick,
            onLedgerDeviceSelected = onLedgerDeviceSelected
        )
    }
}

@UsesSampleValues
@Preview(showBackground = true)
@Composable
fun ChooseLedgerScreenPreview() {
    RadixWalletTheme {
        ChooseLedgerDeviceContent(
            modifier = Modifier.fillMaxSize(),
            ledgerDevices = FactorSource.Ledger.sample.all
                .map {
                    Selectable(it, false)
                }.toPersistentList(),
            onLedgerDeviceSelected = {},
            onUseLedgerContinueClick = {},
            onAddLedgerDeviceClick = {},
            onBackClick = {},
            isAddingNewLinkConnectorInProgress = false,
            uiMessage = null,
            onMessageShown = {}
        )
    }
}
