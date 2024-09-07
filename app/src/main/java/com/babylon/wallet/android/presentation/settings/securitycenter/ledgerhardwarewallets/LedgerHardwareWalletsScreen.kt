package com.babylon.wallet.android.presentation.settings.securitycenter.ledgerhardwarewallets

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import com.babylon.wallet.android.domain.model.toProfileLedgerDeviceModel
import com.babylon.wallet.android.presentation.dialogs.info.GlossaryItem
import com.babylon.wallet.android.presentation.settings.linkedconnectors.AddLinkConnectorViewModel
import com.babylon.wallet.android.presentation.ui.composables.AddLedgerDeviceScreen
import com.babylon.wallet.android.presentation.ui.composables.AddLinkConnectorScreen
import com.babylon.wallet.android.presentation.ui.composables.BackIconType
import com.babylon.wallet.android.presentation.ui.composables.BasicPromptAlertDialog
import com.babylon.wallet.android.presentation.ui.composables.LedgerListItem
import com.babylon.wallet.android.presentation.ui.composables.LinkConnectorScreen
import com.babylon.wallet.android.presentation.ui.composables.RadixCenteredTopAppBar
import com.babylon.wallet.android.presentation.ui.composables.statusBarsAndBanner
import com.radixdlt.sargon.FactorSource
import com.radixdlt.sargon.annotation.UsesSampleValues
import com.radixdlt.sargon.extensions.hex
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.launch
import rdx.works.core.sargon.sample

@Composable
fun LedgerHardwareWalletsScreen(
    modifier: Modifier = Modifier,
    viewModel: LedgerHardwareWalletsViewModel,
    addLedgerDeviceViewModel: AddLedgerDeviceViewModel,
    addLinkConnectorViewModel: AddLinkConnectorViewModel,
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

    Box(
        modifier = modifier
    ) {
        when (state.showContent) {
            LedgerHardwareWalletsUiState.ShowContent.Details -> {
                LedgerHardwareWalletsContent(
                    ledgerDevices = state.ledgerDevices,
                    onAddLedgerDeviceClick = viewModel::onAddLedgerDeviceClick,
                    onBackClick = onBackClick,
                    isNewLinkedConnectorConnected = state.isNewLinkedConnectorConnected
                )
            }

            LedgerHardwareWalletsUiState.ShowContent.AddLedger -> {
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
                    onBackClick = viewModel::onCloseClick,
                    isAddingLedgerDeviceInProgress = addLedgerDeviceState.isAddingLedgerDeviceInProgress,
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
private fun LedgerHardwareWalletsContent(
    ledgerDevices: ImmutableList<FactorSource.Ledger>,
    onAddLedgerDeviceClick: () -> Unit,
    onBackClick: () -> Unit,
    isNewLinkedConnectorConnected: Boolean,
) {
    BackHandler(onBack = onBackClick)

    Scaffold(
        topBar = {
            RadixCenteredTopAppBar(
                title = stringResource(R.string.accountSecuritySettings_ledgerHardwareWallets_title),
                onBackClick = onBackClick,
                windowInsets = WindowInsets.statusBarsAndBanner
            )
        },
        containerColor = RadixTheme.colors.gray5
    ) { padding ->
        Column(modifier = Modifier.padding(padding)) {
            HorizontalDivider(color = RadixTheme.colors.gray5)
            LedgerDeviceDetails(
                modifier = Modifier.fillMaxWidth(),
                ledgerFactorSources = ledgerDevices,
                onAddLedgerDeviceClick = onAddLedgerDeviceClick,
                isNewLinkedConnectorConnected = isNewLinkedConnectorConnected
            )
        }
    }
}

@Composable
private fun LedgerDeviceDetails(
    modifier: Modifier = Modifier,
    ledgerFactorSources: ImmutableList<FactorSource.Ledger>,
    onAddLedgerDeviceClick: () -> Unit,
    isNewLinkedConnectorConnected: Boolean
) {
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingMedium))
        Text(
            modifier = Modifier.padding(horizontal = RadixTheme.dimensions.paddingDefault),
            text = stringResource(id = R.string.ledgerHardwareDevices_subtitleAllLedgers),
            style = RadixTheme.typography.body1HighImportance,
            color = RadixTheme.colors.gray2
        )
        Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingXXLarge))
        if (ledgerFactorSources.isNotEmpty()) {
            LedgerDevicesListContent(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                ledgerDevices = ledgerFactorSources,
                onAddLedgerDeviceClick = onAddLedgerDeviceClick,
                isNewLinkedConnectorConnected = isNewLinkedConnectorConnected
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
                isLoading = isNewLinkedConnectorConnected.not(),
                enabled = isNewLinkedConnectorConnected,
                throttleClicks = true
            )
        }
    }
}

@Composable
private fun LedgerDevicesListContent(
    modifier: Modifier = Modifier,
    ledgerDevices: ImmutableList<FactorSource.Ledger>,
    onAddLedgerDeviceClick: () -> Unit,
    isNewLinkedConnectorConnected: Boolean
) {
    LazyColumn(
        modifier,
        contentPadding = PaddingValues(horizontal = RadixTheme.dimensions.paddingDefault),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        items(
            items = ledgerDevices,
            key = { factorSource: FactorSource.Ledger ->
                factorSource.value.id.body.hex
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
                isLoading = isNewLinkedConnectorConnected.not(),
                enabled = isNewLinkedConnectorConnected
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
            isNewLinkedConnectorConnected = true
        )
    }
}

@UsesSampleValues
@Preview(showBackground = true)
@Composable
fun LedgerHardwareWalletsScreenPreview() {
    RadixWalletTheme {
        LedgerHardwareWalletsContent(
            ledgerDevices = FactorSource.Ledger.sample.all.toPersistentList(),
            onAddLedgerDeviceClick = {},
            onBackClick = {},
            isNewLinkedConnectorConnected = true
        )
    }
}
