@file:OptIn(ExperimentalMaterialApi::class)

package com.babylon.wallet.android.presentation.settings.ledgerfactorsource

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
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
import com.babylon.wallet.android.presentation.ui.composables.InfoLink
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
        onDeleteFactorSource = viewModel::onShowDeleteLedgerDialog,
        onSendAddLedgerRequest = viewModel::onSendAddLedgerRequest,
        onConfirmLedgerName = viewModel::onConfirmLedgerName,
        addLedgerSheetState = state.addLedgerSheetState,
        onSkipLedgerName = viewModel::onSkipLedgerName,
        waitingForLedgerResponse = state.waitingForLedgerResponse,
        onAddP2PLink = onAddP2PLink,
        hasP2pLinks = state.hasP2pLinks
    )
    when (val dialogState = state.deleteLedgerDialogState) {
        is DeleteLedgerDialogState.Shown -> {
            BasicPromptAlertDialog(
                finish = {
                    viewModel.closeDeleteLedgerDialog(if (it) dialogState.factorSourceID else null)
                },
                title = {
                    Text(
                        text = stringResource(id = R.string.remove_ledger_factor_source),
                        style = RadixTheme.typography.body2Header,
                        color = RadixTheme.colors.gray1
                    )
                },
                text = {
                    Text(
                        text = stringResource(id = R.string.you_will_no_longer),
                        style = RadixTheme.typography.body2Regular,
                        color = RadixTheme.colors.gray1
                    )
                },
                confirmText = stringResource(id = R.string.remove)
            )
        }
        else -> {}
    }
}

@Composable
private fun SettingsLinkConnectorContent(
    modifier: Modifier = Modifier,
    onBackClick: () -> Unit,
    ledgerFactorSources: ImmutableList<FactorSource>,
    onDeleteFactorSource: (FactorSource.ID) -> Unit,
    hasP2pLinks: Boolean,
    onAddP2PLink: () -> Unit,
    onSendAddLedgerRequest: () -> Unit,
    addLedgerSheetState: AddLedgerSheetState,
    waitingForLedgerResponse: Boolean,
    onSkipLedgerName: () -> Unit,
    onConfirmLedgerName: (String) -> Unit
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
    DefaultModalSheetLayout(modifier = modifier, sheetState = bottomSheetState, sheetContent = {
        AddLedgerBottomSheet(
            modifier = Modifier
                .fillMaxSize()
                .padding(RadixTheme.dimensions.paddingDefault),
            hasP2pLinks = hasP2pLinks,
            onAddP2PLink = onAddP2PLink,
            onSendAddLedgerRequest = onSendAddLedgerRequest,
            addLedgerSheetState = addLedgerSheetState,
            onConfirmLedgerName = {
                onConfirmLedgerName(it)
                closeSheetCallback()
            },
            onSkipLedgerName = {
                onSkipLedgerName()
                closeSheetCallback()
            },
            waitingForLedgerResponse = waitingForLedgerResponse
        )
    }) {
        Column(modifier = Modifier.fillMaxSize()) {
            RadixCenteredTopAppBar(
                title = stringResource(R.string.ledger_hardware_wallets),
                onBackClick = onBackClick,
                contentColor = RadixTheme.colors.gray1
            )
            Divider(color = RadixTheme.colors.gray5)
            LedgerFactorSourcesDetails(
                modifier = Modifier.fillMaxWidth(),
                ledgerFactorSources = ledgerFactorSources,
                onAddLedger = {
                    scope.launch {
                        bottomSheetState.show()
                    }
                },
                onDeleteFactorSource = onDeleteFactorSource
            )
        }
    }
}

@Composable
private fun LedgerFactorSourcesDetails(
    modifier: Modifier = Modifier,
    ledgerFactorSources: ImmutableList<FactorSource>,
    onAddLedger: () -> Unit,
    onDeleteFactorSource: (FactorSource.ID) -> Unit
) {
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            modifier = Modifier.padding(horizontal = RadixTheme.dimensions.paddingDefault),
            text = stringResource(id = R.string.all_ledgers_info),
            style = RadixTheme.typography.body1HighImportance,
            color = RadixTheme.colors.gray2
        )
        Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingDefault))
        InfoLink(
            stringResource(R.string.what_is_ledger_factor_source),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = RadixTheme.dimensions.paddingDefault)
        )
        if (ledgerFactorSources.isNotEmpty()) {
            LedgerFactorSourcesListContent(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                ledgerFactorSources = ledgerFactorSources,
                onDeleteFactorSource = onDeleteFactorSource
            )
        } else {
            Spacer(modifier = Modifier.weight(1f))
        }
        RadixPrimaryButton(
            modifier = Modifier
                .fillMaxWidth()
                .padding(RadixTheme.dimensions.paddingMedium),
            text = stringResource(id = R.string.add_new_ledger),
            onClick = onAddLedger,
            throttleClicks = true
        )
    }
}

@Composable
private fun LedgerFactorSourcesListContent(
    modifier: Modifier = Modifier,
    ledgerFactorSources: ImmutableList<FactorSource>,
    onDeleteFactorSource: (FactorSource.ID) -> Unit
) {
    LazyColumn(modifier) {
        items(
            items = ledgerFactorSources,
            key = { factorSource: FactorSource ->
                factorSource.id.value
            },
            itemContent = { item ->
                FactorSourceListItem(
                    factorSource = item,
                    onDeleteFactorSource = onDeleteFactorSource
                )
            }
        )
    }
}

@Composable
private fun FactorSourceListItem(
    factorSource: FactorSource,
    modifier: Modifier = Modifier,
    onDeleteFactorSource: (FactorSource.ID) -> Unit,
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
            // TODO we don't know yet what to do with accounts if factor source that was used to create it is deleted
            AnimatedVisibility(visible = false) {
                IconButton(onClick = {
                    onDeleteFactorSource(factorSource.id)
                }) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_delete_24),
                        contentDescription = null,
                        tint = RadixTheme.colors.gray1
                    )
                }
            }
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
            onDeleteFactorSource = {},
            hasP2pLinks = false,
            onAddP2PLink = {},
            waitingForLedgerResponse = false,
            onSkipLedgerName = {},
            addLedgerSheetState = AddLedgerSheetState.Initial,
            onConfirmLedgerName = {},
            onSendAddLedgerRequest = {}
        )
    }
}
