@file:OptIn(ExperimentalMaterialApi::class)

package com.babylon.wallet.android.presentation.settings.ledgerfactorsource

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.babylon.wallet.android.R
import com.babylon.wallet.android.designsystem.composable.RadixPrimaryButton
import com.babylon.wallet.android.designsystem.theme.RadixTheme
import com.babylon.wallet.android.designsystem.theme.RadixWalletTheme
import com.babylon.wallet.android.presentation.model.AddLedgerSheetState
import com.babylon.wallet.android.presentation.ui.composables.AddLedgerBottomSheet
import com.babylon.wallet.android.presentation.ui.composables.BasicPromptAlertDialog
import com.babylon.wallet.android.presentation.ui.composables.DefaultModalSheetLayout
import com.babylon.wallet.android.presentation.ui.composables.RadixCenteredTopAppBar
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.launch
import rdx.works.profile.data.model.factorsources.FactorSource

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
            .background(RadixTheme.colors.defaultBackground),
        onBackClick = onBackClick,
        ledgerFactorSources = state.ledgerFactorSources,
        hasP2pLinks = state.hasP2pLinks,
        onAddP2PLink = onAddP2PLink,
        onSendAddLedgerRequest = viewModel::onSendAddLedgerRequest,
        addLedgerSheetState = state.addLedgerSheetState,
        waitingForLedgerResponse = state.waitingForLedgerResponse,
        onConfirmLedgerName = viewModel::onConfirmLedgerName
    )
}

@OptIn(ExperimentalMaterialApi::class)
@Composable
private fun SettingsLinkConnectorContent(
    modifier: Modifier = Modifier,
    onBackClick: () -> Unit,
    ledgerFactorSources: ImmutableList<FactorSource>,
    hasP2pLinks: Boolean,
    onAddP2PLink: () -> Unit,
    onSendAddLedgerRequest: () -> Unit,
    addLedgerSheetState: AddLedgerSheetState,
    waitingForLedgerResponse: Boolean,
    onConfirmLedgerName: (String) -> Unit
) {
    var showNoP2pLinksDialog by remember {
        mutableStateOf(false)
    }
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
    DefaultModalSheetLayout(modifier = modifier, sheetState = bottomSheetState, sheetContent = {
        AddLedgerBottomSheet(
            modifier = Modifier
                .fillMaxSize()
                .padding(RadixTheme.dimensions.paddingDefault),
            onSendAddLedgerRequest = onSendAddLedgerRequest,
            addLedgerSheetState = addLedgerSheetState,
            onConfirmLedgerName = {
                onConfirmLedgerName(it)
                closeSheetCallback()
            },
            waitingForLedgerResponse = waitingForLedgerResponse
        )
    }) {
        Column(modifier = Modifier.fillMaxSize()) {
            RadixCenteredTopAppBar(
                title = stringResource(R.string.settings_ledgerHardwareWallets),
                onBackClick = onBackClick,
                contentColor = RadixTheme.colors.gray1
            )
            Divider(color = RadixTheme.colors.gray5)
            LedgerFactorSourcesDetails(
                modifier = Modifier.fillMaxWidth(),
                ledgerFactorSources = ledgerFactorSources,
                onAddLedger = {
                    if (hasP2pLinks) {
                        scope.launch {
                            bottomSheetState.show()
                        }
                    } else {
                        showNoP2pLinksDialog = true
                    }
                }
            )
        }
    }
    if (showNoP2pLinksDialog) {
        BasicPromptAlertDialog(
            finish = {
                if (it) {
                    onAddP2PLink()
                }
                showNoP2pLinksDialog = false
            },
            title = {
                Text(
                    text = stringResource(id = R.string.ledgerHardwareDevices_linkConnectorAlert_title),
                    style = RadixTheme.typography.body2Header,
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
            confirmText = stringResource(id = R.string.common_continue)
        )
    }
}

@Composable
private fun LedgerFactorSourcesDetails(
    modifier: Modifier = Modifier,
    ledgerFactorSources: ImmutableList<FactorSource>,
    onAddLedger: () -> Unit
) {
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            modifier = Modifier.padding(horizontal = RadixTheme.dimensions.paddingDefault),
            text = stringResource(id = R.string.ledgerHardwareDevices_subtitleAllLedgers),
            style = RadixTheme.typography.body1HighImportance,
            color = RadixTheme.colors.gray2
        )
        Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingDefault))
        if (ledgerFactorSources.isNotEmpty()) {
            LedgerFactorSourcesListContent(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                ledgerFactorSources = ledgerFactorSources
            )
        } else {
            Spacer(modifier = Modifier.weight(1f))
        }
        RadixPrimaryButton(
            modifier = Modifier
                .fillMaxWidth()
                .padding(RadixTheme.dimensions.paddingMedium),
            text = stringResource(id = R.string.ledgerHardwareDevices_addNewLedger),
            onClick = onAddLedger,
            throttleClicks = true
        )
    }
}

@Composable
private fun LedgerFactorSourcesListContent(
    modifier: Modifier = Modifier,
    ledgerFactorSources: ImmutableList<FactorSource>
) {
    LazyColumn(modifier) {
        items(
            items = ledgerFactorSources,
            key = { factorSource: FactorSource ->
                factorSource.id.value
            },
            itemContent = { item ->
                FactorSourceListItem(
                    factorSource = item
                )
            }
        )
    }
}

@Composable
private fun FactorSourceListItem(
    factorSource: FactorSource,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(RadixTheme.dimensions.paddingDefault),
            horizontalArrangement = Arrangement.spacedBy(RadixTheme.dimensions.paddingSmall),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                modifier = Modifier.weight(1f),
                text = "${factorSource.label} (${factorSource.description})",
                style = RadixTheme.typography.body2Regular,
                color = RadixTheme.colors.gray2
            )
        }
        Divider(Modifier.fillMaxWidth(), color = RadixTheme.colors.gray4)
    }
}

@Preview(showBackground = true)
@Composable
fun SettingsScreenLinkConnectorWithActiveConnectorPreview() {
    RadixWalletTheme {
        SettingsLinkConnectorContent(
            onBackClick = {},
            ledgerFactorSources = persistentListOf(
                FactorSource.ledger(
                    id = FactorSource.ID("1"),
                    model = FactorSource.LedgerHardwareWallet.DeviceModel.NanoS,
                    name = "My Ledger",
                )
            ),
            hasP2pLinks = false,
            onAddP2PLink = {},
            onSendAddLedgerRequest = {},
            addLedgerSheetState = AddLedgerSheetState.Connect,
            waitingForLedgerResponse = false
        ) {}
    }
}
