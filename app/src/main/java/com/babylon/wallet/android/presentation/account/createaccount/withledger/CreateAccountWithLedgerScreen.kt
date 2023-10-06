package com.babylon.wallet.android.presentation.account.createaccount.withledger

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
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
import com.babylon.wallet.android.presentation.settings.accountsecurity.ledgerhardwarewallets.AddLedgerDeviceViewModel
import com.babylon.wallet.android.presentation.settings.appsettings.linkedconnectors.AddLinkConnectorViewModel
import com.babylon.wallet.android.presentation.ui.composables.AddLedgerDeviceScreen
import com.babylon.wallet.android.presentation.ui.composables.AddLinkConnectorScreen
import com.babylon.wallet.android.presentation.ui.composables.BackIconType
import com.babylon.wallet.android.presentation.ui.composables.BasicPromptAlertDialog
import com.babylon.wallet.android.presentation.ui.composables.ChooseLedgerDeviceSection
import com.babylon.wallet.android.presentation.ui.composables.LinkConnectorScreen
import com.babylon.wallet.android.presentation.ui.composables.RadixCenteredTopAppBar
import com.babylon.wallet.android.utils.biometricAuthenticateSuspend
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
    val context = LocalContext.current
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
    val promptState = state.showLinkConnectorPromptState
    if (promptState is ShowLinkConnectorPromptState.Show) {
        BasicPromptAlertDialog(
            finish = {
                viewModel.dismissConnectorPrompt(it, promptState.source)
            },
            title = {
                Text(
                    text = stringResource(id = R.string.ledgerHardwareDevices_linkConnectorAlert_title),
                    style = RadixTheme.typography.body1Header,
                    color = RadixTheme.colors.gray1
                )
            },
            text = {
                Text(
                    text = stringResource(id = R.string.ledgerHardwareDevices_linkConnectorAlert_message),
                    style = RadixTheme.typography.body2Regular,
                    color = RadixTheme.colors.gray1
                )
            },
            confirmText = stringResource(id = R.string.ledgerHardwareDevices_linkConnectorAlert_continue)
        )
    }
    when (val showContent = state.showContent) {
        CreateAccountWithLedgerUiState.ShowContent.ChooseLedger -> {
            ChooseLedgerDeviceContent(
                modifier = modifier,
                onBackClick = onBackClick,
                ledgerDevices = state.ledgerDevices,
                onLedgerDeviceSelected = viewModel::onLedgerDeviceSelected,
                onAddLedgerDeviceClick = viewModel::onAddLedgerDeviceClick,
                onUseLedgerContinueClick = {
                    viewModel.onUseLedgerContinueClick(deviceBiometricAuthenticationProvider = {
                        context.biometricAuthenticateSuspend()
                    })
                }
            )
        }

        is CreateAccountWithLedgerUiState.ShowContent.LinkNewConnector -> {
            LinkConnectorScreen(
                modifier = Modifier.fillMaxSize(),
                onLinkConnectorClick = {
                    viewModel.onLinkConnectorClick(showContent.addDeviceAfterLinking)
                },
                onCloseClick = viewModel::onCloseClick
            )
        }

        is CreateAccountWithLedgerUiState.ShowContent.AddLinkConnector -> {
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
                        if (showContent.addDeviceAfterLinking) {
                            viewModel.showAddLedgerDeviceContent()
                        } else {
                            viewModel.onCloseClick()
                        }
                    }
                },
                onNewConnectorCloseClick = {
                    addLinkConnectorViewModel.onCloseClick()
                    viewModel.onCloseClick()
                },
                invalidConnectionPassword = addLinkConnectorState.invalidConnectionPassword,
                onInvalidConnectionPasswordDismissed = addLinkConnectorViewModel::onInvalidConnectionPasswordShown
            )
        }

        CreateAccountWithLedgerUiState.ShowContent.AddLedger -> {
            AddLedgerDeviceScreen(
                modifier = Modifier,
                showContent = addLedgerDeviceState.showContent,
                deviceModel = addLedgerDeviceState.newConnectedLedgerDevice?.model?.toProfileLedgerDeviceModel()?.value,
                uiMessage = addLedgerDeviceState.uiMessage,
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
                },
                onMessageShown = addLedgerDeviceViewModel::onMessageShown,
                connectorExtensionConnected = addLedgerDeviceState.connectorExtensionConnected,
//                hasP2PLinks = state.hasP2PLinks
            )
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

    Scaffold(
        modifier = modifier,
        topBar = {
            RadixCenteredTopAppBar(
                title = stringResource(id = R.string.empty),
                onBackClick = onBackClick,
                backIconType = BackIconType.Close,
                windowInsets = WindowInsets.statusBars
            )
        },
        bottomBar = {
            RadixPrimaryButton(
                text = stringResource(id = R.string.ledgerHardwareDevices_continueWithLedger),
                onClick = onUseLedgerContinueClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        horizontal = RadixTheme.dimensions.paddingSemiLarge,
                        vertical = RadixTheme.dimensions.paddingDefault
                    )
                    .navigationBarsPadding(),
                enabled = ledgerDevices.any { it.selected }
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
