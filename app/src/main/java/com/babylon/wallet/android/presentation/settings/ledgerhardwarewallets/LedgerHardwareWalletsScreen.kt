package com.babylon.wallet.android.presentation.settings.ledgerhardwarewallets

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Divider
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
import com.babylon.wallet.android.designsystem.composable.RadixPrimaryButton
import com.babylon.wallet.android.designsystem.composable.RadixSecondaryButton
import com.babylon.wallet.android.designsystem.theme.RadixTheme
import com.babylon.wallet.android.designsystem.theme.RadixWalletTheme
import com.babylon.wallet.android.domain.SampleDataProvider
import com.babylon.wallet.android.domain.model.toProfileLedgerDeviceModel
import com.babylon.wallet.android.presentation.common.UiMessage
import com.babylon.wallet.android.presentation.model.AddLedgerSheetState
import com.babylon.wallet.android.presentation.settings.linkedconnectors.AddLinkConnectorScreen
import com.babylon.wallet.android.presentation.settings.linkedconnectors.AddLinkConnectorViewModel
import com.babylon.wallet.android.presentation.ui.composables.AddLedgerContent
import com.babylon.wallet.android.presentation.ui.composables.BackIconType
import com.babylon.wallet.android.presentation.ui.composables.LedgerListItem
import com.babylon.wallet.android.presentation.ui.composables.LinkConnectorScreen
import com.babylon.wallet.android.presentation.ui.composables.RadixCenteredTopAppBar
import com.babylon.wallet.android.presentation.ui.composables.SnackbarUiMessageHandler
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.launch
import rdx.works.profile.data.model.factorsources.LedgerHardwareWalletFactorSource

@Composable
fun LedgerHardwareWalletsScreen(
    viewModel: LedgerHardwareWalletsViewModel,
    addLinkConnectorViewModel: AddLinkConnectorViewModel,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val addLinkConnectorState by addLinkConnectorViewModel.state.collectAsStateWithLifecycle()
    val coroutineScope = rememberCoroutineScope()

    Box(
        modifier = modifier
            .navigationBarsPadding()
            .fillMaxSize()
            .background(RadixTheme.colors.gray5)
    ) {
        when (state.showContent) {
            LedgerHardwareWalletsUiState.ShowContent.Details -> {
                LedgerHardwareWalletsContent(
                    ledgerDevices = state.ledgerDevices,
                    onAddLedgerDeviceClick = viewModel::onAddLedgerDeviceClick,
                    onBackClick = onBackClick,
                )
            }

            LedgerHardwareWalletsUiState.ShowContent.AddLedger -> {
                AddLedgerDeviceScreen(
                    modifier = Modifier,
                    deviceModel = state.recentlyConnectedLedgerDevice?.model?.toProfileLedgerDeviceModel()?.value,
                    onSendAddLedgerRequest = viewModel::onSendAddLedgerRequest,
                    addLedgerSheetState = state.addLedgerSheetState,
                    onConfirmLedgerName = viewModel::onConfirmLedgerName,
                    onCloseAddLedgerClick = viewModel::onCloseClick,
                    waitingForLedgerResponse = state.waitingForLedgerResponse,
                    uiMessage = state.uiMessage,
                    onMessageShown = viewModel::onMessageShown
                )
            }

            LedgerHardwareWalletsUiState.ShowContent.LinkNewConnector -> {
                LinkConnectorScreen(
                    modifier = Modifier.fillMaxSize(),
                    onLinkConnectorClick = viewModel::onLinkConnectorClick,
                    onBackClick = viewModel::onCloseClick
                )
            }

            LedgerHardwareWalletsUiState.ShowContent.AddLinkConnector -> {
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
                        viewModel.onNewConnectorCloseClick()
                    }
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
) {
    BackHandler(onBack = onBackClick)

    Column {
        RadixCenteredTopAppBar(
            title = stringResource(R.string.settings_ledgerHardwareWallets),
            onBackClick = onBackClick,
            contentColor = RadixTheme.colors.gray1,
            modifier = Modifier.background(RadixTheme.colors.defaultBackground)
        )
        Divider(color = RadixTheme.colors.gray5)
        LedgerDeviceDetails(
            modifier = Modifier.fillMaxWidth(),
            ledgerFactorSources = ledgerDevices,
            onAddLedgerDeviceClick = onAddLedgerDeviceClick
        )
    }
}

@Composable
private fun AddLedgerDeviceScreen(
    modifier: Modifier,
    deviceModel: String?,
    onSendAddLedgerRequest: () -> Unit,
    addLedgerSheetState: AddLedgerSheetState,
    onConfirmLedgerName: (String) -> Unit,
    onCloseAddLedgerClick: () -> Unit,
    waitingForLedgerResponse: Boolean,
    uiMessage: UiMessage?,
    onMessageShown: () -> Unit
) {
    Box {
        AddLedgerContent(
            modifier = modifier,
            deviceModel = deviceModel,
            onSendAddLedgerRequest = onSendAddLedgerRequest,
            addLedgerSheetState = addLedgerSheetState,
            onConfirmLedgerName = {
                onConfirmLedgerName(it)
            },
            backIconType = BackIconType.Back,
            onClose = onCloseAddLedgerClick,
            waitingForLedgerResponse = waitingForLedgerResponse
        )
        SnackbarUiMessageHandler(
            message = uiMessage,
            onMessageShown = onMessageShown
        )
    }
}

@Composable
private fun LedgerDeviceDetails(
    modifier: Modifier = Modifier,
    ledgerFactorSources: ImmutableList<LedgerHardwareWalletFactorSource>,
    onAddLedgerDeviceClick: () -> Unit
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
                onAddLedgerDeviceClick = onAddLedgerDeviceClick
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
            RadixPrimaryButton(
                text = stringResource(id = R.string.ledgerHardwareDevices_addNewLedger),
                onClick = onAddLedgerDeviceClick,
                modifier = Modifier
                    .fillMaxWidth(0.7f)
                    .imePadding()
            )
        }
    }
}

@Composable
private fun LedgerDevicesListContent(
    modifier: Modifier = Modifier,
    ledgerDevices: ImmutableList<LedgerHardwareWalletFactorSource>,
    onAddLedgerDeviceClick: () -> Unit
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
                throttleClicks = true
            )
        }
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
        )
    }
}
