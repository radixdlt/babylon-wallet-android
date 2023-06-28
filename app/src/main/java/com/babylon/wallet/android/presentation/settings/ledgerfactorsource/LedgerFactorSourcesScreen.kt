@file:OptIn(ExperimentalMaterialApi::class)

package com.babylon.wallet.android.presentation.settings.ledgerfactorsource

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
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.ModalBottomSheetValue
import androidx.compose.material.rememberModalBottomSheetState
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
import com.babylon.wallet.android.domain.model.toProfileLedgerDeviceModel
import com.babylon.wallet.android.presentation.common.UiMessage
import com.babylon.wallet.android.presentation.model.AddLedgerSheetState
import com.babylon.wallet.android.presentation.ui.composables.AddLedgerBottomSheet
import com.babylon.wallet.android.presentation.ui.composables.DefaultModalSheetLayout
import com.babylon.wallet.android.presentation.ui.composables.LedgerListItem
import com.babylon.wallet.android.presentation.ui.composables.RadixCenteredTopAppBar
import com.babylon.wallet.android.presentation.ui.composables.SnackbarUiMessageHandler
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.launch
import rdx.works.profile.data.model.factorsources.FactorSource
import rdx.works.profile.data.model.factorsources.LedgerHardwareWalletFactorSource

@Composable
fun LedgerFactorSourcesScreen(
    viewModel: LedgerFactorSourcesViewModel,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
    onAddP2PLink: () -> Unit,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    SettingsLinkConnectorContent(
        modifier = modifier
            .navigationBarsPadding()
            .fillMaxSize()
            .background(RadixTheme.colors.gray5),
        onBackClick = onBackClick,
        ledgerFactorSources = state.ledgerFactorSources,
        onAddP2PLink = onAddP2PLink,
        onSendAddLedgerRequest = viewModel::onSendAddLedgerRequest,
        addLedgerSheetState = state.addLedgerSheetState,
        waitingForLedgerResponse = state.waitingForLedgerResponse,
        onConfirmLedgerName = viewModel::onConfirmLedgerName,
        deviceModel = state.recentlyConnectedLedgerDevice?.model?.toProfileLedgerDeviceModel()?.value,
        uiMessage = state.uiMessage,
        onMessageShown = viewModel::onMessageShown
    )
}

@OptIn(ExperimentalMaterialApi::class)
@Composable
private fun SettingsLinkConnectorContent(
    modifier: Modifier = Modifier,
    onBackClick: () -> Unit,
    ledgerFactorSources: ImmutableList<LedgerHardwareWalletFactorSource>,
    onAddP2PLink: () -> Unit,
    onSendAddLedgerRequest: () -> Unit,
    addLedgerSheetState: AddLedgerSheetState,
    waitingForLedgerResponse: Boolean,
    onConfirmLedgerName: (String) -> Unit,
    deviceModel: String?,
    uiMessage: UiMessage?,
    onMessageShown: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val bottomSheetState =
        rememberModalBottomSheetState(initialValue = ModalBottomSheetValue.Hidden, skipHalfExpanded = true)
    val closeSheetCallback = {
        scope.launch {
            bottomSheetState.hide()
        }
    }
    BackHandler {
        if (bottomSheetState.isVisible) {
            closeSheetCallback()
        } else {
            onBackClick()
        }
    }
    Box(modifier = modifier) {
        DefaultModalSheetLayout(modifier = Modifier.fillMaxSize(), sheetState = bottomSheetState, sheetContent = {
            AddLedgerBottomSheet(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(RadixTheme.dimensions.paddingDefault),
                deviceModel = deviceModel,
                onSendAddLedgerRequest = onSendAddLedgerRequest,
                addLedgerSheetState = addLedgerSheetState,
                onConfirmLedgerName = {
                    onConfirmLedgerName(it)
                    closeSheetCallback()
                },
                onSheetClose = { closeSheetCallback() },
                waitingForLedgerResponse = waitingForLedgerResponse,
                onAddP2PLink = onAddP2PLink
            )
        }) {
            Column(modifier = Modifier.fillMaxSize()) {
                RadixCenteredTopAppBar(
                    title = stringResource(R.string.settings_ledgerHardwareWallets),
                    onBackClick = onBackClick,
                    contentColor = RadixTheme.colors.gray1,
                    modifier = Modifier.background(RadixTheme.colors.defaultBackground)
                )
                Divider(color = RadixTheme.colors.gray5)
                LedgerFactorSourcesDetails(
                    modifier = Modifier.fillMaxWidth(),
                    ledgerFactorSources = ledgerFactorSources,
                    onAddLedger = {
                        scope.launch {
                            bottomSheetState.show()
                        }
                    }
                )
            }
        }
        SnackbarUiMessageHandler(
            message = uiMessage,
            onMessageShown = onMessageShown
        )
    }
}

@Composable
private fun LedgerFactorSourcesDetails(
    modifier: Modifier = Modifier,
    ledgerFactorSources: ImmutableList<LedgerHardwareWalletFactorSource>,
    onAddLedger: () -> Unit
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
            LedgerFactorSourcesListContent(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                ledgerFactorSources = ledgerFactorSources,
                onAddLedger = onAddLedger
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
                modifier = Modifier
                    .fillMaxWidth(0.7f)
                    .imePadding(),
                onClick = onAddLedger,
                text = stringResource(id = R.string.ledgerHardwareDevices_addNewLedger)
            )
        }
    }
}

@Composable
private fun LedgerFactorSourcesListContent(
    modifier: Modifier = Modifier,
    ledgerFactorSources: ImmutableList<LedgerHardwareWalletFactorSource>,
    onAddLedger: () -> Unit
) {
    LazyColumn(
        modifier,
        contentPadding = PaddingValues(horizontal = RadixTheme.dimensions.paddingDefault),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        items(
            items = ledgerFactorSources,
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
                onClick = onAddLedger,
                throttleClicks = true
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun SettingsScreenLinkConnectorWithActiveConnectorPreview() {
    RadixWalletTheme {
        SettingsLinkConnectorContent(
            onBackClick = {},
            ledgerFactorSources = persistentListOf(
                LedgerHardwareWalletFactorSource.newSource(
                    deviceID = FactorSource.HexCoded32Bytes("5f07ec336e9e7891bff04004c817201e73c097b6b1e1b3a26bc205e0010196f5"),
                    model = LedgerHardwareWalletFactorSource.DeviceModel.NANO_S,
                    name = "My Ledger",
                )
            ),
            onAddP2PLink = {},
            onSendAddLedgerRequest = {},
            addLedgerSheetState = AddLedgerSheetState.Connect,
            waitingForLedgerResponse = false,
            onConfirmLedgerName = {},
            deviceModel = null,
            uiMessage = null
        ) {}
    }
}
