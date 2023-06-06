@file:OptIn(ExperimentalMaterialApi::class, ExperimentalMaterialApi::class, ExperimentalMaterialApi::class)

package com.babylon.wallet.android.presentation.createaccount.withledger

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.ModalBottomSheetValue
import androidx.compose.material.rememberModalBottomSheetState
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.babylon.wallet.android.designsystem.R
import com.babylon.wallet.android.designsystem.composable.RadixPrimaryButton
import com.babylon.wallet.android.designsystem.composable.RadixSecondaryButton
import com.babylon.wallet.android.designsystem.theme.RadixTheme
import com.babylon.wallet.android.designsystem.theme.RadixWalletTheme
import com.babylon.wallet.android.presentation.common.FullscreenCircularProgressContent
import com.babylon.wallet.android.presentation.model.AddLedgerSheetState
import com.babylon.wallet.android.presentation.ui.composables.AddLedgerBottomSheet
import com.babylon.wallet.android.presentation.ui.composables.BasicPromptAlertDialog
import com.babylon.wallet.android.presentation.ui.composables.DefaultModalSheetLayout
import com.babylon.wallet.android.presentation.ui.composables.LedgerSelector
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.launch
import rdx.works.profile.data.model.factorsources.FactorSource

@Composable
fun CreateAccountWithLedgerScreen(
    viewModel: CreateAccountWithLedgerViewModel,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
    goBackToCreateAccount: () -> Unit,
    onAddP2PLink: () -> Unit
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    if (state.loading) {
        FullscreenCircularProgressContent()
    } else {
        LaunchedEffect(Unit) {
            viewModel.oneOffEvent.collect { event ->
                when (event) {
                    CreateAccountWithLedgerEvent.DerivedPublicKeyForAccount -> goBackToCreateAccount()
                }
            }
        }
        CreateAccountWithLedgerContent(
            modifier = modifier,
            onBackClick = onBackClick,
            ledgerFactorSources = state.ledgerFactorSources,
            selectedFactorSourceID = state.selectedFactorSourceID,
            onLedgerFactorSourceSelected = viewModel::onLedgerFactorSourceSelected,
            onSendAddLedgerRequest = viewModel::onSendAddLedgerRequest,
            addLedgerSheetState = state.addLedgerSheetState,
            onConfirmLedgerName = viewModel::onConfirmLedgerName,
            onUseLedger = viewModel::onUseLedger,
            waitingForLedgerResponse = state.waitingForLedgerResponse,
            hasP2PLinks = state.hasP2pLinks,
            onAddP2PLink = onAddP2PLink
        )
    }
}

@Composable
fun CreateAccountWithLedgerContent(
    modifier: Modifier,
    onBackClick: () -> Unit,
    ledgerFactorSources: ImmutableList<FactorSource>,
    selectedFactorSourceID: FactorSource.ID?,
    onLedgerFactorSourceSelected: (FactorSource) -> Unit,
    onSendAddLedgerRequest: () -> Unit,
    addLedgerSheetState: AddLedgerSheetState,
    onConfirmLedgerName: (String) -> Unit,
    onUseLedger: () -> Unit,
    waitingForLedgerResponse: Boolean,
    hasP2PLinks: Boolean,
    onAddP2PLink: () -> Unit
) {
    var showNoP2PLinksDialog by remember {
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
                onSendAddLedgerRequest = onSendAddLedgerRequest,
                addLedgerSheetState = addLedgerSheetState,
                onConfirmLedgerName = {
                    onConfirmLedgerName(it)
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
                        text = stringResource(
                            id = com.babylon.wallet.android.R.string.createEntity_ledger_createAccount
                        ),
                        style = RadixTheme.typography.title,
                        color = RadixTheme.colors.gray1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingMedium))
                    if (ledgerFactorSources.isEmpty()) {
                        Text(
                            text = stringResource(id = com.babylon.wallet.android.R.string.ledgerHardwareDevices_subtitleNoLedgers),
                            style = RadixTheme.typography.body1Header,
                            color = RadixTheme.colors.gray1,
                            overflow = TextOverflow.Ellipsis,
                            textAlign = TextAlign.Center
                        )
                    } else {
                        Text(
                            text = stringResource(
                                id = com.babylon.wallet.android.R.string.ledgerHardwareDevices_navigationTitleAllowSelection
                            ),
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
                        if (hasP2PLinks) {
                            scope.launch {
                                bottomSheetState.show()
                            }
                        } else {
                            showNoP2PLinksDialog = true
                        }
                    },
                    text = stringResource(id = com.babylon.wallet.android.R.string.ledgerHardwareDevices_addNewLedger)
                )
                AnimatedVisibility(visible = ledgerFactorSources.isNotEmpty()) {
                    RadixPrimaryButton(
                        modifier = Modifier
                            .fillMaxWidth()
                            .imePadding(),
                        onClick = onUseLedger,
                        text = stringResource(id = com.babylon.wallet.android.R.string.ledgerHardwareDevices_continueWithLedger)
                    )
                }
            }
            if (waitingForLedgerResponse) {
                FullscreenCircularProgressContent()
            }
        }
        if (showNoP2PLinksDialog) {
            BasicPromptAlertDialog(
                finish = {
                    if (it) {
                        onAddP2PLink()
                    }
                    showNoP2PLinksDialog = false
                },
                title = {
                    Text(
                        text = stringResource(id = com.babylon.wallet.android.R.string.ledgerHardwareDevices_linkConnectorAlert_title),
                        style = RadixTheme.typography.body2Header,
                        color = RadixTheme.colors.gray1
                    )
                },
                text = {
                    Text(
                        text = stringResource(id = com.babylon.wallet.android.R.string.ledgerHardwareDevices_linkConnectorAlert_message),
                        style = RadixTheme.typography.body2Regular,
                        color = RadixTheme.colors.gray1
                    )
                },
                confirmText = stringResource(id = com.babylon.wallet.android.R.string.ledgerHardwareDevices_linkConnectorAlert_continue)
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun CreateAccountWithLedgerContentPreview() {
    RadixWalletTheme {
        CreateAccountWithLedgerContent(
            modifier = Modifier.fillMaxSize(),
            onBackClick = {},
            ledgerFactorSources = persistentListOf(),
            selectedFactorSourceID = null,
            onLedgerFactorSourceSelected = {},
            onSendAddLedgerRequest = {},
            addLedgerSheetState = AddLedgerSheetState.Connect,
            onConfirmLedgerName = {},
            onUseLedger = {},
            waitingForLedgerResponse = false,
            hasP2PLinks = false,
            onAddP2PLink = {}
        )
    }
}
