@file:OptIn(ExperimentalMaterialApi::class)

package com.babylon.wallet.android.presentation.createaccount.withledger

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.ModalBottomSheetValue
import androidx.compose.material.rememberModalBottomSheetState
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.babylon.wallet.android.designsystem.R
import com.babylon.wallet.android.designsystem.composable.RadixPrimaryButton
import com.babylon.wallet.android.designsystem.theme.RadixTheme
import com.babylon.wallet.android.designsystem.theme.RadixWalletTheme
import com.babylon.wallet.android.domain.model.Selectable
import com.babylon.wallet.android.domain.model.toProfileLedgerDeviceModel
import com.babylon.wallet.android.presentation.common.FullscreenCircularProgressContent
import com.babylon.wallet.android.presentation.common.UiMessage
import com.babylon.wallet.android.presentation.model.AddLedgerSheetState
import com.babylon.wallet.android.presentation.ui.composables.AddLedgerBottomSheet
import com.babylon.wallet.android.presentation.ui.composables.DefaultModalSheetLayout
import com.babylon.wallet.android.presentation.ui.composables.LinkConnectorSection
import com.babylon.wallet.android.presentation.ui.composables.SnackbarUiMessageHandler
import com.babylon.wallet.android.presentation.ui.composables.UseOrAddLedgerSection
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.launch
import rdx.works.profile.data.model.factorsources.LedgerHardwareWalletFactorSource

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
            onLedgerFactorSourceSelected = viewModel::onLedgerFactorSourceSelected,
            onSendAddLedgerRequest = viewModel::onSendAddLedgerRequest,
            addLedgerSheetState = state.addLedgerSheetState,
            onConfirmLedgerName = viewModel::onConfirmLedgerName,
            onUseLedger = viewModel::onUseLedger,
            waitingForLedgerResponse = state.waitingForLedgerResponse,
            hasP2PLinks = state.hasP2pLinks,
            onAddP2PLink = onAddP2PLink,
            deviceModel = state.recentlyConnectedLedgerDevice?.model?.toProfileLedgerDeviceModel()?.value,
            uiMessage = state.uiMessage,
            onMessageShown = viewModel::onMessageShown
        )
    }
}

@Composable
fun CreateAccountWithLedgerContent(
    modifier: Modifier,
    onBackClick: () -> Unit,
    ledgerFactorSources: ImmutableList<Selectable<LedgerHardwareWalletFactorSource>>,
    onLedgerFactorSourceSelected: (LedgerHardwareWalletFactorSource) -> Unit,
    onSendAddLedgerRequest: () -> Unit,
    addLedgerSheetState: AddLedgerSheetState,
    onConfirmLedgerName: (String) -> Unit,
    onUseLedger: () -> Unit,
    waitingForLedgerResponse: Boolean,
    hasP2PLinks: Boolean,
    onAddP2PLink: () -> Unit,
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
    Box(
        modifier = modifier
            .navigationBarsPadding()
            .background(RadixTheme.colors.defaultBackground)
            .fillMaxSize()
    ) {
        DefaultModalSheetLayout(
            modifier = Modifier.fillMaxSize(),
            sheetState = bottomSheetState,
            sheetContent = {
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
            }
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                val contentModifier = Modifier
                    .fillMaxSize()
                    .padding(
                        horizontal = RadixTheme.dimensions.paddingLarge,
                        vertical = RadixTheme.dimensions.paddingDefault
                    )
                Column(
                    modifier = Modifier.fillMaxSize()
                ) {
                    Row(modifier = Modifier.fillMaxWidth()) {
                        IconButton(onClick = onBackClick) {
                            Icon(
                                painterResource(id = R.drawable.ic_arrow_back),
                                tint = RadixTheme.colors.gray1,
                                contentDescription = "navigate back"
                            )
                        }
                    }
                    if (!hasP2PLinks) {
                        LinkConnectorSection(contentModifier, onAddP2PLink)
                    } else {
                        UseOrAddLedgerSection(
                            modifier = contentModifier.weight(1f),
                            ledgerFactorSources = ledgerFactorSources,
                            onAddLedger = {
                                scope.launch {
                                    bottomSheetState.show()
                                }
                            },
                            onLedgerFactorSourceSelected = onLedgerFactorSourceSelected
                        )
                        RadixPrimaryButton(
                            text = stringResource(id = com.babylon.wallet.android.R.string.ledgerHardwareDevices_continueWithLedger),
                            onClick = onUseLedger,
                            modifier = Modifier
                                .fillMaxWidth()
                                .imePadding(),
                            enabled = ledgerFactorSources.any { it.selected }
                        )
                    }
                }
                if (waitingForLedgerResponse) {
                    FullscreenCircularProgressContent()
                }
            }
        }
        SnackbarUiMessageHandler(
            message = uiMessage,
            onMessageShown = onMessageShown
        )
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
            onLedgerFactorSourceSelected = {},
            onSendAddLedgerRequest = {},
            addLedgerSheetState = AddLedgerSheetState.Connect,
            onConfirmLedgerName = {},
            onUseLedger = {},
            waitingForLedgerResponse = false,
            hasP2PLinks = false,
            onAddP2PLink = {},
            deviceModel = null,
            uiMessage = null,
            onMessageShown = {}
        )
    }
}
