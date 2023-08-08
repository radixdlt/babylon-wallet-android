package com.babylon.wallet.android.presentation.createaccount.withledger

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.babylon.wallet.android.R
import com.babylon.wallet.android.designsystem.composable.RadixPrimaryButton
import com.babylon.wallet.android.designsystem.theme.RadixTheme
import com.babylon.wallet.android.designsystem.theme.RadixWalletTheme
import com.babylon.wallet.android.domain.SampleDataProvider
import com.babylon.wallet.android.domain.model.Selectable
import com.babylon.wallet.android.domain.model.toProfileLedgerDeviceModel
import com.babylon.wallet.android.presentation.settings.ledgerhardwarewallets.AddLedgerDeviceViewModel
import com.babylon.wallet.android.presentation.settings.linkedconnectors.AddLinkConnectorViewModel
import com.babylon.wallet.android.presentation.ui.composables.AddLedgerDeviceScreen
import com.babylon.wallet.android.presentation.ui.composables.AddLinkConnectorScreen
import com.babylon.wallet.android.presentation.ui.composables.BackIconType
import com.babylon.wallet.android.presentation.ui.composables.ChooseLedgerDeviceSection
import com.babylon.wallet.android.presentation.ui.composables.LinkConnectorScreen
import com.babylon.wallet.android.presentation.ui.composables.RadixCenteredTopAppBar
import com.babylon.wallet.android.presentation.ui.composables.SnackbarUiMessageHandler
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.launch
import rdx.works.profile.data.model.factorsources.LedgerHardwareWalletFactorSource

@Composable
fun CreateAccountWithLedgerScreen(
    viewModel: CreateAccountWithLedgerViewModel,
    addLedgerDeviceViewModel: AddLedgerDeviceViewModel,
    addLinkConnectorViewModel: AddLinkConnectorViewModel,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
    goBackToCreateAccount: () -> Unit,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val addLedgerDeviceState by addLedgerDeviceViewModel.state.collectAsStateWithLifecycle()
    val addLinkConnectorState by addLinkConnectorViewModel.state.collectAsStateWithLifecycle()
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        viewModel.oneOffEvent.collect { event ->
            when (event) {
                CreateAccountWithLedgerEvent.DerivedPublicKeyForAccount -> goBackToCreateAccount()
            }
        }
    }

    when (state.showContent) {
        CreateAccountWithLedgerUiState.ShowContent.ChooseLedger -> {
            ChooseLedgerDeviceContent(
                modifier = modifier,
                onBackClick = onBackClick,
                ledgerDevices = state.ledgerDevices,
                onLedgerDeviceSelected = viewModel::onLedgerDeviceSelected,
                onAddLedgerDeviceClick = viewModel::onAddLedgerDeviceClick,
                onUseLedgerContinueClick = viewModel::onUseLedgerContinueClick
            )
        }

        CreateAccountWithLedgerUiState.ShowContent.LinkNewConnector -> {
            LinkConnectorScreen(
                modifier = Modifier.fillMaxSize(),
                onLinkConnectorClick = viewModel::onLinkConnectorClick,
                onCloseClick = viewModel::onCloseClick
            )
        }

        CreateAccountWithLedgerUiState.ShowContent.AddLinkConnector -> {
            AddLinkConnectorScreen(
                modifier = Modifier,
                showContent = addLinkConnectorState.showContent,
                isLoading = addLinkConnectorState.isLoading,
                onQrCodeScanned = addLinkConnectorViewModel::onQrCodeScanned,
                onConnectorDisplayNameChanged = addLinkConnectorViewModel::onConnectorDisplayNameChanged,
                connectorDisplayName = addLinkConnectorState.connectorDisplayName,
                isNewConnectorContinueButtonEnabled = addLinkConnectorState.isContinueButtonEnabled,
                onNewConnectorContinueClick = {
                    coroutineScope.launch {
                        addLinkConnectorViewModel.onContinueClick()
                        viewModel.onAddLedgerDeviceClick()
                    }
                },
                onNewConnectorCloseClick = {
                    addLinkConnectorViewModel.onCloseClick()
                    viewModel.onCloseClick()
                }
            )
        }

        CreateAccountWithLedgerUiState.ShowContent.AddLedger -> {
            Box(
                modifier = modifier
                    .navigationBarsPadding()
                    .fillMaxSize()
            ) {
                AddLedgerDeviceScreen(
                    modifier = Modifier,
                    showContent = addLedgerDeviceState.showContent,
                    deviceModel = addLedgerDeviceState.newConnectedLedgerDevice?.model?.toProfileLedgerDeviceModel()?.value,
                    onSendAddLedgerRequestClick = addLedgerDeviceViewModel::onSendAddLedgerRequestClick,
                    onConfirmLedgerNameClick = {
                        coroutineScope.launch {
                            addLedgerDeviceViewModel.onConfirmLedgerNameClick(it)
                            viewModel.onCloseClick()
                        }
                    },
                    backIconType = BackIconType.Back,
                    onClose = {
                        addLedgerDeviceViewModel.initState()
                        viewModel.onCloseClick()
                    },
                    waitingForLedgerResponse = false,
                    onBackClick = {
                        addLedgerDeviceViewModel.initState()
                        viewModel.onCloseClick()
                    }
                )
                SnackbarUiMessageHandler(
                    message = addLedgerDeviceState.uiMessage,
                    onMessageShown = addLedgerDeviceViewModel::onMessageShown
                )
            }
        }
    }
}

@Composable
private fun ChooseLedgerDeviceContent(
    modifier: Modifier,
    ledgerDevices: ImmutableList<Selectable<LedgerHardwareWalletFactorSource>>,
    onLedgerDeviceSelected: (LedgerHardwareWalletFactorSource) -> Unit,
    onUseLedgerContinueClick: () -> Unit,
    onAddLedgerDeviceClick: () -> Unit,
    onBackClick: () -> Unit
) {
    BackHandler { onBackClick() }

    Box(
        modifier = modifier
            .navigationBarsPadding()
            .background(RadixTheme.colors.defaultBackground)
            .fillMaxSize()
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            RadixCenteredTopAppBar(
                title = stringResource(id = R.string.empty),
                onBackClick = onBackClick,
                contentColor = RadixTheme.colors.gray1,
                modifier = Modifier
                    .background(RadixTheme.colors.defaultBackground)
                    .padding(
                        start = RadixTheme.dimensions.paddingMedium,
                        top = RadixTheme.dimensions.paddingMedium
                    ),
                backIconType = BackIconType.Close
            )
            ChooseLedgerDeviceSection(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(
                        horizontal = RadixTheme.dimensions.paddingLarge,
                        vertical = RadixTheme.dimensions.paddingDefault
                    )
                    .weight(1f),
                ledgerDevices = ledgerDevices,
                onAddLedgerDeviceClick = onAddLedgerDeviceClick,
                onLedgerDeviceSelected = onLedgerDeviceSelected
            )
            RadixPrimaryButton(
                text = stringResource(id = R.string.ledgerHardwareDevices_continueWithLedger),
                onClick = onUseLedgerContinueClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(RadixTheme.dimensions.paddingSemiLarge)
                    .imePadding(),
                enabled = ledgerDevices.any { it.selected }
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun CreateAccountWithLedgerScreenPreview() {
    RadixWalletTheme {
        ChooseLedgerDeviceContent(
            modifier = Modifier.fillMaxSize(),
            ledgerDevices = SampleDataProvider()
                .ledgerFactorSourcesSample
                .map {
                    Selectable(it, false)
                }.toPersistentList(),
            onLedgerDeviceSelected = {},
            onUseLedgerContinueClick = {},
            onAddLedgerDeviceClick = {},
            onBackClick = {}
        )
    }
}
