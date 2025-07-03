package com.babylon.wallet.android.presentation.settings.securitycenter.securityfactors.ledgerdevice

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.babylon.wallet.android.R
import com.babylon.wallet.android.designsystem.composable.RadixSecondaryButton
import com.babylon.wallet.android.designsystem.theme.RadixTheme
import com.babylon.wallet.android.designsystem.theme.RadixWalletTheme
import com.babylon.wallet.android.presentation.dialogs.info.GlossaryItem
import com.babylon.wallet.android.presentation.settings.linkedconnectors.AddLinkConnectorViewModel
import com.babylon.wallet.android.presentation.settings.securitycenter.securityfactors.composables.FactorSourcesList
import com.babylon.wallet.android.presentation.ui.composables.AddLedgerDeviceScreen
import com.babylon.wallet.android.presentation.ui.composables.AddLinkConnectorScreen
import com.babylon.wallet.android.presentation.ui.composables.BackIconType
import com.babylon.wallet.android.presentation.ui.composables.BasicPromptAlertDialog
import com.babylon.wallet.android.presentation.ui.composables.LinkConnectorScreen
import com.babylon.wallet.android.presentation.ui.composables.RadixCenteredTopAppBar
import com.babylon.wallet.android.presentation.ui.composables.statusBarsAndBanner
import com.babylon.wallet.android.presentation.ui.model.factors.FactorSourceCard
import com.radixdlt.sargon.Account
import com.radixdlt.sargon.FactorSourceId
import com.radixdlt.sargon.FactorSourceKind
import com.radixdlt.sargon.MnemonicWithPassphrase
import com.radixdlt.sargon.Persona
import com.radixdlt.sargon.annotation.UsesSampleValues
import com.radixdlt.sargon.extensions.init
import com.radixdlt.sargon.samples.sample
import com.radixdlt.sargon.samples.sampleMainnet
import com.radixdlt.sargon.samples.sampleStokenet
import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.launch

@Composable
fun LedgerDevicesScreen(
    modifier: Modifier = Modifier,
    viewModel: LedgerDevicesViewModel,
    addLedgerDeviceViewModel: AddLedgerDeviceViewModel,
    addLinkConnectorViewModel: AddLinkConnectorViewModel,
    onNavigateToLedgerFactorSourceDetails: (factorSourceId: FactorSourceId) -> Unit,
    onInfoClick: (GlossaryItem) -> Unit,
    onBackClick: () -> Unit
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val addLedgerDeviceState by addLedgerDeviceViewModel.state.collectAsStateWithLifecycle()
    val addLinkConnectorState by addLinkConnectorViewModel.state.collectAsStateWithLifecycle()
    val coroutineScope = rememberCoroutineScope()

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
                    color = RadixTheme.colors.text
                )
            },
            message = {
                Text(
                    text = stringResource(id = R.string.ledgerHardwareDevices_linkConnectorAlert_message),
                    style = RadixTheme.typography.body2Regular,
                    color = RadixTheme.colors.text
                )
            },
            confirmText = stringResource(id = R.string.ledgerHardwareDevices_linkConnectorAlert_continue)
        )
    }

    LaunchedEffect(Unit) {
        viewModel.oneOffEvent.collect { event ->
            when (event) {
                is LedgerDevicesViewModel.Event.NavigateToLedgerFactorSourceDetails -> {
                    onNavigateToLedgerFactorSourceDetails(event.factorSourceId)
                }
            }
        }
    }

    LaunchedEffect(Unit) {
        addLinkConnectorViewModel.oneOffEvent.collect { event ->
            when (event) {
                is AddLinkConnectorViewModel.Event.Close -> {
                    if (event.isLinkedConnectorAdded) {
                        viewModel.disableAddLedgerButtonUntilConnectionIsEstablished()
                    } else {
                        viewModel.onNewConnectorCloseClick()
                    }
                }
            }
        }
    }

    Box(modifier = modifier) {
        when (state.showContent) {
            LedgerDevicesViewModel.State.ShowContent.Details -> {
                LedgerDevicesContent(
                    ledgerFactorSources = state.ledgerFactorSources,
                    onLedgerFactorSourceClick = viewModel::onLedgerFactorSourceClick,
                    onAddLedgerDeviceClick = viewModel::onAddLedgerDeviceClick,
                    isNewLinkedConnectorConnected = state.isNewLinkedConnectorConnected,
                    onBackClick = onBackClick
                )
            }

            LedgerDevicesViewModel.State.ShowContent.AddLedger -> {
                AddLedgerDeviceScreen(
                    showContent = addLedgerDeviceState.showContent,
                    uiMessage = addLedgerDeviceState.uiMessage,
                    deviceModel = addLedgerDeviceState.newConnectedLedgerDevice?.model?.toProfileLedgerDeviceModel(),
                    onSendAddLedgerRequestClick = addLedgerDeviceViewModel::onSendAddLedgerRequestClick,
                    onConfirmLedgerNameClick = {
                        coroutineScope.launch {
                            addLedgerDeviceViewModel.onConfirmLedgerNameClick(it)
                            viewModel.onCloseClick()
                        }
                    },
                    backIconType = BackIconType.Back,
                    onMessageShown = addLedgerDeviceViewModel::onMessageShown,
                    onClose = viewModel::onCloseClick,
                    waitingForLedgerResponse = false,
                    isAddingLedgerDeviceInProgress = addLedgerDeviceState.isAddingLedgerDeviceInProgress,
                )
            }

            is LedgerDevicesViewModel.State.ShowContent.LinkNewConnector -> {
                LinkConnectorScreen(
                    modifier = Modifier.fillMaxSize(),
                    onLinkConnectorClick = viewModel::onLinkConnectorClick,
                    onCloseClick = viewModel::onCloseClick
                )
            }

            is LedgerDevicesViewModel.State.ShowContent.AddLinkConnector -> {
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
        }
    }
}

@Composable
private fun LedgerDevicesContent(
    ledgerFactorSources: PersistentList<FactorSourceCard>,
    isNewLinkedConnectorConnected: Boolean,
    onLedgerFactorSourceClick: (FactorSourceId) -> Unit,
    onAddLedgerDeviceClick: () -> Unit,
    onBackClick: () -> Unit
) {
    BackHandler(onBack = onBackClick)

    Scaffold(
        topBar = {
            RadixCenteredTopAppBar(
                title = stringResource(R.string.factorSources_card_ledgerTitle),
                onBackClick = onBackClick,
                windowInsets = WindowInsets.statusBarsAndBanner
            )
        },
        containerColor = RadixTheme.colors.backgroundSecondary
    ) { padding ->
        Column(
            modifier = Modifier.padding(padding),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            HorizontalDivider(color = RadixTheme.colors.divider)

            FactorSourcesList(
                factorSources = ledgerFactorSources,
                factorSourceDescriptionText = R.string.factorSources_card_ledgerDescription,
                addFactorSourceButtonContent = {
                    RadixSecondaryButton(
                        modifier = Modifier
                            .fillMaxWidth()
                            .wrapContentWidth(align = Alignment.CenterHorizontally),
                        text = stringResource(id = R.string.factorSources_list_ledgerAdd),
                        onClick = onAddLedgerDeviceClick,
                        throttleClicks = true,
                        isLoading = isNewLinkedConnectorConnected.not(),
                        enabled = isNewLinkedConnectorConnected
                    )
                },
                onFactorSourceClick = onLedgerFactorSourceClick
            )
        }
    }
}

@UsesSampleValues
@Preview(showBackground = true)
@Composable
fun LedgerDevicesScreenPreview() {
    RadixWalletTheme {
        LedgerDevicesContent(
            ledgerFactorSources = persistentListOf(
                FactorSourceCard(
                    id = FactorSourceId.Hash.init(
                        kind = FactorSourceKind.LEDGER_HQ_HARDWARE_WALLET,
                        mnemonicWithPassphrase = MnemonicWithPassphrase.sample(),
                    ),
                    name = "My Ledger",
                    includeDescription = false,
                    lastUsedOn = "Today",
                    kind = FactorSourceKind.LEDGER_HQ_HARDWARE_WALLET,
                    messages = persistentListOf(),
                    accounts = persistentListOf(Account.sampleMainnet()),
                    personas = persistentListOf(
                        Persona.sampleMainnet(),
                        Persona.sampleStokenet()
                    ),
                    hasHiddenEntities = true,
                    supportsBabylon = true,
                    isEnabled = true
                ),
                FactorSourceCard(
                    id = FactorSourceId.Hash.init(
                        kind = FactorSourceKind.LEDGER_HQ_HARDWARE_WALLET,
                        mnemonicWithPassphrase = MnemonicWithPassphrase.sample(),
                    ),
                    name = "XXX Ledger",
                    includeDescription = false,
                    lastUsedOn = "Last year",
                    kind = FactorSourceKind.LEDGER_HQ_HARDWARE_WALLET,
                    messages = persistentListOf(),
                    accounts = persistentListOf(),
                    personas = persistentListOf(),
                    hasHiddenEntities = false,
                    supportsBabylon = true,
                    isEnabled = true
                ),
                FactorSourceCard(
                    id = FactorSourceId.Hash.init(
                        kind = FactorSourceKind.LEDGER_HQ_HARDWARE_WALLET,
                        mnemonicWithPassphrase = MnemonicWithPassphrase.sample(),
                    ),
                    name = "Gate13",
                    includeDescription = false,
                    lastUsedOn = "Last year",
                    kind = FactorSourceKind.LEDGER_HQ_HARDWARE_WALLET,
                    messages = persistentListOf(),
                    accounts = persistentListOf(Account.sampleMainnet()),
                    personas = persistentListOf(),
                    hasHiddenEntities = false,
                    supportsBabylon = true,
                    isEnabled = true
                )
            ),
            isNewLinkedConnectorConnected = true,
            onLedgerFactorSourceClick = {},
            onAddLedgerDeviceClick = {},
            onBackClick = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
fun LedgerDevicesEmptyScreenPreview() {
    RadixWalletTheme {
        LedgerDevicesContent(
            ledgerFactorSources = persistentListOf(),
            isNewLinkedConnectorConnected = true,
            onLedgerFactorSourceClick = {},
            onAddLedgerDeviceClick = {},
            onBackClick = {}
        )
    }
}
