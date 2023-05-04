@file:OptIn(ExperimentalMaterialApi::class, ExperimentalMaterialApi::class)

package com.babylon.wallet.android.presentation.createaccount.addledger

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.DropdownMenuItem
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.ModalBottomSheetValue
import androidx.compose.material.rememberModalBottomSheetState
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.babylon.wallet.android.designsystem.R
import com.babylon.wallet.android.designsystem.composable.RadixPrimaryButton
import com.babylon.wallet.android.designsystem.composable.RadixSecondaryButton
import com.babylon.wallet.android.designsystem.composable.RadixTextField
import com.babylon.wallet.android.designsystem.theme.RadixTheme
import com.babylon.wallet.android.designsystem.theme.RadixWalletTheme
import com.babylon.wallet.android.presentation.common.FullscreenCircularProgressContent
import com.babylon.wallet.android.presentation.ui.composables.DefaultModalSheetLayout
import com.babylon.wallet.android.presentation.ui.modifier.throttleClickable
import com.babylon.wallet.android.utils.addedOnTimestampFormatted
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.launch
import rdx.works.profile.data.model.factorsources.FactorSource

@Composable
fun AddLedgerScreen(
    viewModel: AddLedgerViewModel,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
    onAddP2PLink: () -> Unit,
    goBackToCreateAccount: () -> Unit
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    if (state.loading) {
        FullscreenCircularProgressContent()
    } else {
        LaunchedEffect(Unit) {
            viewModel.oneOffEvent.collect { event ->
                when (event) {
                    AddLedgerEvent.DerivedPublicKeyForAccount -> goBackToCreateAccount()
                }
            }
        }
        AddLedgerContent(
            modifier = modifier,
            onBackClick = onBackClick,
            ledgerFactorSources = state.ledgerFactorSources,
            hasP2pLinks = state.hasP2pLinks,
            selectedFactorSourceID = state.selectedFactorSourceID,
            onLedgerFactorSourceSelected = viewModel::onLedgerFactorSourceSelected,
            onAddP2PLink = onAddP2PLink,
            onSendAddLedgerRequest = viewModel::onSendAddLedgerRequest,
            addLedgerSheetState = state.addLedgerSheetState,
            onConfirmLedgerName = viewModel::onConfirmLedgerName,
            onSkipLedgerName = viewModel::onSkipLedgerName,
            onUseLedger = viewModel::onUseLedger,
            waitingForLedgerResponse = state.waitingForLedgerResponse
        )
    }
}

@Composable
fun AddLedgerContent(
    modifier: Modifier,
    onBackClick: () -> Unit,
    ledgerFactorSources: ImmutableList<FactorSource>,
    hasP2pLinks: Boolean,
    selectedFactorSourceID: FactorSource.ID?,
    onLedgerFactorSourceSelected: (FactorSource) -> Unit,
    onAddP2PLink: () -> Unit,
    onSendAddLedgerRequest: () -> Unit,
    addLedgerSheetState: AddLedgerSheetState,
    onConfirmLedgerName: (String) -> Unit,
    onSkipLedgerName: () -> Unit,
    onUseLedger: () -> Unit,
    waitingForLedgerResponse: Boolean
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
    DefaultModalSheetLayout(
        modifier = modifier
            .navigationBarsPadding()
            .background(RadixTheme.colors.defaultBackground)
            .fillMaxSize(),
        sheetState = bottomSheetState,
        sheetContent = {
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
        }
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(
                        horizontal = RadixTheme.dimensions.paddingLarge,
                        vertical = RadixTheme.dimensions.paddingDefault
                    )
            ) {
                IconButton(onClick = onBackClick) {
                    Icon(
                        painterResource(id = R.drawable.ic_close),
                        tint = RadixTheme.colors.gray1,
                        contentDescription = "navigate back"
                    )
                }
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .verticalScroll(rememberScrollState()),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = stringResource(id = com.babylon.wallet.android.R.string.create_ledger_account),
                        style = RadixTheme.typography.title,
                        color = RadixTheme.colors.gray1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingMedium))
                    if (ledgerFactorSources.isEmpty()) {
                        Text(
                            text = stringResource(id = com.babylon.wallet.android.R.string.you_have_no_ledgers_added),
                            style = RadixTheme.typography.body1Header,
                            color = RadixTheme.colors.gray1,
                            overflow = TextOverflow.Ellipsis,
                            textAlign = TextAlign.Center
                        )
                    } else {
                        Text(
                            text = stringResource(id = com.babylon.wallet.android.R.string.select_ledger_device),
                            style = RadixTheme.typography.body1Header,
                            color = RadixTheme.colors.gray1,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingMedium))
                        LedgerSelector(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(RadixTheme.dimensions.paddingDefault),
                            selectedLedgerFactorSourceID = selectedFactorSourceID,
                            ledgerFactorSources = ledgerFactorSources,
                            onLedgerFactorSourceSelected = onLedgerFactorSourceSelected
                        )
                    }
                }
                RadixSecondaryButton(
                    modifier = Modifier
                        .fillMaxWidth()
                        .imePadding(),
                    onClick = {
                        scope.launch {
                            bottomSheetState.show()
                        }
                    },
                    text = stringResource(id = com.babylon.wallet.android.R.string.add_new_ledger)
                )
                AnimatedVisibility(visible = ledgerFactorSources.isNotEmpty()) {
                    RadixPrimaryButton(
                        modifier = Modifier
                            .fillMaxWidth()
                            .imePadding(),
                        onClick = onUseLedger,
                        text = stringResource(id = com.babylon.wallet.android.R.string.use_ledger)
                    )
                }
            }
            if (waitingForLedgerResponse) {
                FullscreenCircularProgressContent()
            }
        }
    }
}

@Composable
fun AddLedgerBottomSheet(
    modifier: Modifier,
    hasP2pLinks: Boolean,
    onAddP2PLink: () -> Unit,
    onSendAddLedgerRequest: () -> Unit,
    addLedgerSheetState: AddLedgerSheetState,
    onConfirmLedgerName: (String) -> Unit,
    onSkipLedgerName: () -> Unit,
    waitingForLedgerResponse: Boolean
) {
    Box(modifier = modifier) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(RadixTheme.dimensions.paddingSmall),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            var ledgerNameValue by remember {
                mutableStateOf("")
            }
            when (addLedgerSheetState) {
                AddLedgerSheetState.Initial -> {
                    if (!hasP2pLinks) {
                        Text(
                            text = stringResource(id = com.babylon.wallet.android.R.string.found_no_radix_connect_connections),
                            style = RadixTheme.typography.body1Header,
                            color = RadixTheme.colors.gray1,
                            overflow = TextOverflow.Ellipsis,
                            textAlign = TextAlign.Center
                        )
                        RadixSecondaryButton(
                            modifier = Modifier.fillMaxWidth(),
                            onClick = onAddP2PLink,
                            text = stringResource(id = com.babylon.wallet.android.R.string.add_new_p2p_link)
                        )
                    }
                    Text(
                        text = stringResource(id = com.babylon.wallet.android.R.string.connect_the_ledger_device),
                        style = RadixTheme.typography.body1Regular,
                        color = RadixTheme.colors.gray1,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.weight(1f))
                    RadixSecondaryButton(
                        modifier = Modifier.fillMaxWidth(),
                        onClick = onSendAddLedgerRequest,
                        text = stringResource(id = com.babylon.wallet.android.R.string.send_add_ledger_request),
                        enabled = hasP2pLinks
                    )
                }
                AddLedgerSheetState.InputLedgerName -> {
                    RadixTextField(
                        modifier = Modifier.fillMaxWidth(),
                        onValueChanged = { ledgerNameValue = it },
                        value = ledgerNameValue,
                        leftLabel = stringResource(id = com.babylon.wallet.android.R.string.name_this_ledger),
                        hint = stringResource(id = com.babylon.wallet.android.R.string.ledger_hint),
                        optionalHint = stringResource(id = com.babylon.wallet.android.R.string.ledger_name_bottom_hint)
                    )
                    RadixPrimaryButton(
                        modifier = Modifier.fillMaxWidth(),
                        text = stringResource(com.babylon.wallet.android.R.string.confirm_name),
                        onClick = {
                            onConfirmLedgerName(ledgerNameValue)
                            ledgerNameValue = ""
                        },
                        throttleClicks = true
                    )
                    RadixSecondaryButton(
                        modifier = Modifier
                            .fillMaxWidth()
                            .imePadding(),
                        text = stringResource(com.babylon.wallet.android.R.string.skip),
                        onClick = onSkipLedgerName,
                        throttleClicks = true
                    )
                }
            }
        }
        if (waitingForLedgerResponse) {
            FullscreenCircularProgressContent()
        }
    }
}

@Composable
private fun LedgerSelector(
    modifier: Modifier,
    selectedLedgerFactorSourceID: FactorSource.ID?,
    ledgerFactorSources: ImmutableList<FactorSource>,
    onLedgerFactorSourceSelected: (FactorSource) -> Unit
) {
    var dropdownMenuExpanded by remember { mutableStateOf(false) }
    Column(modifier) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight()
                .border(1.dp, RadixTheme.colors.gray1, shape = RadixTheme.shapes.roundedRectMedium)
                .clip(RadixTheme.shapes.roundedRectMedium)
                .throttleClickable { dropdownMenuExpanded = !dropdownMenuExpanded }
                .padding(RadixTheme.dimensions.paddingDefault),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val selectedFactorSource = ledgerFactorSources.firstOrNull { it.id == selectedLedgerFactorSourceID }
            val selectedLedgerDescription = stringResource(
                id = com.babylon.wallet.android.R.string.selected_ledger_description,
                selectedFactorSource?.label.orEmpty(),
                selectedFactorSource?.addedOnTimestampFormatted().orEmpty()
            )
            Text(
                modifier = Modifier.weight(1f),
                text = selectedLedgerDescription,
                style = RadixTheme.typography.body1Regular,
                color = RadixTheme.colors.gray1
            )
            Icon(painter = painterResource(id = R.drawable.ic_arrow_down), contentDescription = null, tint = RadixTheme.colors.gray1)
        }
        DropdownMenu(
            modifier = Modifier
                .background(RadixTheme.colors.defaultBackground)
                .height(200.dp),
            expanded = dropdownMenuExpanded,
            onDismissRequest = { dropdownMenuExpanded = false }
        ) {
            ledgerFactorSources.forEach { factorSource ->
                DropdownMenuItem(onClick = {
                    onLedgerFactorSourceSelected(factorSource)
                    dropdownMenuExpanded = false
                }) {
                    Column {
                        Text(
                            factorSource.label,
                            textAlign = TextAlign.Start,
                            style = RadixTheme.typography.body1Regular,
                            color = RadixTheme.colors.gray1
                        )
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun AddLedgerContentPreview() {
    RadixWalletTheme {
        AddLedgerContent(
            modifier = Modifier.fillMaxSize(),
            onBackClick = {},
            ledgerFactorSources = persistentListOf(),
            hasP2pLinks = false,
            selectedFactorSourceID = null,
            onLedgerFactorSourceSelected = {},
            onAddP2PLink = {},
            onSendAddLedgerRequest = {},
            addLedgerSheetState = AddLedgerSheetState.Initial,
            onConfirmLedgerName = {},
            onSkipLedgerName = {},
            onUseLedger = {},
            waitingForLedgerResponse = false
        )
    }
}
