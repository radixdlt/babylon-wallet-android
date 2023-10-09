package com.babylon.wallet.android.presentation.settings.accountsecurity.ledgerhardwarewallets

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Divider
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.babylon.wallet.android.R
import com.babylon.wallet.android.designsystem.composable.RadixSecondaryButton
import com.babylon.wallet.android.designsystem.theme.RadixTheme
import com.babylon.wallet.android.designsystem.theme.RadixWalletTheme
import com.babylon.wallet.android.domain.SampleDataProvider
import com.babylon.wallet.android.domain.model.toProfileLedgerDeviceModel
import com.babylon.wallet.android.presentation.settings.appsettings.linkedconnectors.AddLinkConnectorViewModel
import com.babylon.wallet.android.presentation.ui.composables.AddLedgerDeviceScreen
import com.babylon.wallet.android.presentation.ui.composables.AddLinkConnectorScreen
import com.babylon.wallet.android.presentation.ui.composables.BackIconType
import com.babylon.wallet.android.presentation.ui.composables.BasicPromptAlertDialog
import com.babylon.wallet.android.presentation.ui.composables.LedgerListItem
import com.babylon.wallet.android.presentation.ui.composables.LinkConnectorScreen
import com.babylon.wallet.android.presentation.ui.composables.RadixCenteredTopAppBar
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.launch
import rdx.works.profile.data.model.factorsources.LedgerHardwareWalletFactorSource

@Composable
fun LedgerHardwareWalletsScreen(
    modifier: Modifier = Modifier,
    viewModel: LedgerHardwareWalletsViewModel,
    addLedgerDeviceViewModel: AddLedgerDeviceViewModel,
    addLinkConnectorViewModel: AddLinkConnectorViewModel,
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
    Box(
        modifier = modifier
    ) {
        when (state.showContent) {
            LedgerHardwareWalletsUiState.ShowContent.Details -> {
                LedgerHardwareWalletsContent(
                    ledgerDevices = state.ledgerDevices,
                    onAddLedgerDeviceClick = viewModel::onAddLedgerDeviceClick,
                    onBackClick = onBackClick,
                    addLedgerEnabled = state.addLedgerEnabled
                )
            }

            LedgerHardwareWalletsUiState.ShowContent.AddLedger -> {
                AddLedgerDeviceScreen(
                    showContent = addLedgerDeviceState.showContent,
                    uiMessage = addLedgerDeviceState.uiMessage,
                    deviceModel = addLedgerDeviceState.newConnectedLedgerDevice?.model?.toProfileLedgerDeviceModel()?.value,
                    onSendAddLedgerRequestClick = addLedgerDeviceViewModel::onSendAddLedgerRequestClick,
                    onConfirmLedgerNameClick = {
                        coroutineScope.launch {
                            addLedgerDeviceViewModel.onConfirmLedgerNameClick(it)
                            viewModel.onCloseClick()
                        }
                    },
                    backIconType = BackIconType.Back,
                    onMessageShown = addLedgerDeviceViewModel::onMessageShown,
                    onClose = {
                        addLedgerDeviceViewModel.initState()
                        viewModel.onCloseClick()
                    },
                    waitingForLedgerResponse = false,
                    onBackClick = {
                        addLedgerDeviceViewModel.initState()
                        viewModel.onCloseClick()
                    },
                    isLinkConnectionEstablished = addLedgerDeviceState.isLinkConnectionEstablished
                )
            }

            is LedgerHardwareWalletsUiState.ShowContent.LinkNewConnector -> {
                LinkConnectorScreen(
                    modifier = Modifier.fillMaxSize(),
                    onLinkConnectorClick = viewModel::onLinkConnectorClick,
                    onCloseClick = viewModel::onCloseClick
                )
            }

            is LedgerHardwareWalletsUiState.ShowContent.AddLinkConnector -> {
                AddLinkConnectorScreen(
                    modifier = Modifier,
                    showContent = addLinkConnectorState.showContent,
                    isLoading = addLinkConnectorState.isLoading,
                    onQrCodeScanned = addLinkConnectorViewModel::onQrCodeScanned,
                    onConnectorDisplayNameChanged = addLinkConnectorViewModel::onConnectorDisplayNameChanged,
                    connectorDisplayName = addLinkConnectorState.connectorDisplayName,
                    isNewConnectorContinueButtonEnabled = addLinkConnectorState.isContinueButtonEnabled,
                    onNewConnectorContinueClick = {
                        addLinkConnectorViewModel.onContinueClick()
                        viewModel.disableAddLedgerButtonUntilConnectionIsEstablished()
                    },
                    onNewConnectorCloseClick = {
                        addLinkConnectorViewModel.onCloseClick()
                        viewModel.onNewConnectorCloseClick()
                    },
                    invalidConnectionPassword = addLinkConnectorState.invalidConnectionPassword,
                    onInvalidConnectionPasswordDismissed = addLinkConnectorViewModel::onInvalidConnectionPasswordShown
                )
            }
        }
    }
}

@Composable
private fun LedgerHardwareWalletsContent(
    ledgerDevices: ImmutableList<LedgerHardwareWalletFactorSource>,
    onAddLedgerDeviceClick: () -> Unit,
    onBackClick: () -> Unit,
    addLedgerEnabled: Boolean,
) {
    BackHandler(onBack = onBackClick)

    Scaffold(
        topBar = {
            RadixCenteredTopAppBar(
                title = stringResource(R.string.settings_ledgerHardwareWallets),
                onBackClick = onBackClick,
                windowInsets = WindowInsets.statusBars
            )
        },
        containerColor = RadixTheme.colors.gray5
    ) { padding ->
        Column(modifier = Modifier.padding(padding)) {
            Divider(color = RadixTheme.colors.gray5)
            LedgerDeviceDetails(
                modifier = Modifier.fillMaxWidth(),
                ledgerFactorSources = ledgerDevices,
                onAddLedgerDeviceClick = onAddLedgerDeviceClick,
                addLedgerEnabled = addLedgerEnabled
            )
        }
    }
}

@Composable
private fun LedgerDeviceDetails(
    modifier: Modifier = Modifier,
    ledgerFactorSources: ImmutableList<LedgerHardwareWalletFactorSource>,
    onAddLedgerDeviceClick: () -> Unit,
    addLedgerEnabled: Boolean
) {
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingMedium))
        Text(
            modifier = Modifier.padding(horizontal = RadixTheme.dimensions.paddingDefault),
            text = stringResource(id = R.string.ledgerHardwareDevices_subtitleAllLedgers),
            style = RadixTheme.typography.body1HighImportance,
            color = RadixTheme.colors.gray2
        )
        Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingXLarge))
        if (ledgerFactorSources.isNotEmpty()) {
            LedgerDevicesListContent(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                ledgerDevices = ledgerFactorSources,
                onAddLedgerDeviceClick = onAddLedgerDeviceClick,
                addLedgerEnabled = addLedgerEnabled
            )
        } else {
            Text(
                modifier = Modifier
                    .padding(horizontal = RadixTheme.dimensions.paddingDefault)
                    .fillMaxWidth()
                    .background(RadixTheme.colors.defaultBackground, RadixTheme.shapes.roundedRectSmall)
                    .padding(RadixTheme.dimensions.paddingLarge),
                text = stringResource(id = R.string.ledgerHardwareDevices_subtitleNoLedgers),
                style = RadixTheme.typography.body1Header,
                color = RadixTheme.colors.gray2,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingLarge))
            RadixSecondaryButton(
                text = stringResource(id = R.string.ledgerHardwareDevices_addNewLedger),
                onClick = onAddLedgerDeviceClick,
                modifier = Modifier
                    .fillMaxWidth(0.7f)
                    .imePadding(),
                enabled = addLedgerEnabled,
                throttleClicks = true
            )
        }
    }
}

@Composable
private fun LedgerDevicesListContent(
    modifier: Modifier = Modifier,
    ledgerDevices: ImmutableList<LedgerHardwareWalletFactorSource>,
    onAddLedgerDeviceClick: () -> Unit,
    addLedgerEnabled: Boolean
) {
    LazyColumn(
        modifier,
        contentPadding = PaddingValues(horizontal = RadixTheme.dimensions.paddingDefault),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        items(
            items = ledgerDevices,
            key = { factorSource: LedgerHardwareWalletFactorSource ->
                factorSource.id.body.value
            },
            itemContent = { item ->
                LedgerListItem(
                    ledgerFactorSource = item,
                    modifier = Modifier
                        .shadow(elevation = 4.dp, shape = RadixTheme.shapes.roundedRectSmall)
                        .fillMaxWidth()
                        .background(RadixTheme.colors.defaultBackground, shape = RadixTheme.shapes.roundedRectSmall)
                        .padding(RadixTheme.dimensions.paddingLarge),

                )
                Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingMedium))
            }
        )
        item {
            Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingDefault))
            RadixSecondaryButton(
                modifier = Modifier
                    .fillMaxWidth(0.7f)
                    .padding(bottom = RadixTheme.dimensions.paddingMedium),
                text = stringResource(id = R.string.ledgerHardwareDevices_addNewLedger),
                onClick = onAddLedgerDeviceClick,
                throttleClicks = true,
                enabled = addLedgerEnabled
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun LedgerHardwareWalletsScreenEmptyPreview() {
    RadixWalletTheme {
        LedgerHardwareWalletsContent(
            ledgerDevices = persistentListOf(),
            onAddLedgerDeviceClick = {},
            onBackClick = {},
            addLedgerEnabled = true
        )
    }
}

@Preview(showBackground = true)
@Composable
fun LedgerHardwareWalletsScreenPreview() {
    RadixWalletTheme {
        LedgerHardwareWalletsContent(
            ledgerDevices = SampleDataProvider().ledgerFactorSourcesSample.toPersistentList(),
            onAddLedgerDeviceClick = {},
            onBackClick = {},
            addLedgerEnabled = true
        )
    }
}
