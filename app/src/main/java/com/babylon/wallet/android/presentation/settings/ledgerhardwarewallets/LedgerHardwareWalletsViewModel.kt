package com.babylon.wallet.android.presentation.settings.ledgerhardwarewallets

import androidx.lifecycle.viewModelScope
import com.babylon.wallet.android.data.dapp.LedgerMessenger
import com.babylon.wallet.android.presentation.common.StateViewModel
import com.babylon.wallet.android.presentation.common.UiMessage
import com.babylon.wallet.android.presentation.common.UiState
import com.babylon.wallet.android.presentation.createaccount.withledger.AddLedgerDeviceDelegate
import com.babylon.wallet.android.presentation.model.AddLedgerSheetState
import com.babylon.wallet.android.presentation.model.LedgerDeviceUiModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import rdx.works.profile.data.model.factorsources.LedgerHardwareWalletFactorSource
import rdx.works.profile.domain.AddLedgerFactorSourceUseCase
import rdx.works.profile.domain.GetProfileUseCase
import rdx.works.profile.domain.p2pLinks
import javax.inject.Inject

@HiltViewModel
class LedgerHardwareWalletsViewModel @Inject constructor(
    private val getProfileUseCase: GetProfileUseCase,
    ledgerMessenger: LedgerMessenger,
    addLedgerFactorSourceUseCase: AddLedgerFactorSourceUseCase
) : StateViewModel<LedgerHardwareWalletsUiState>() {

    private val addLedgerDeviceDelegate = AddLedgerDeviceDelegate(
        getProfileUseCase = getProfileUseCase,
        ledgerMessenger = ledgerMessenger,
        addLedgerFactorSourceUseCase = addLedgerFactorSourceUseCase,
        scope = viewModelScope
    )

    override fun initialState(): LedgerHardwareWalletsUiState = LedgerHardwareWalletsUiState()

    init {
        viewModelScope.launch {
            addLedgerDeviceDelegate.state.collect { delegateState ->
                _state.update { uiState ->
                    uiState.copy(
                        loading = delegateState.loading,
                        addLedgerSheetState = delegateState.addLedgerSheetState,
                        ledgerDevices = delegateState.ledgerDevices.toPersistentList(),
                        waitingForLedgerResponse = delegateState.waitingForLedgerResponse,
                        recentlyConnectedLedgerDevice = delegateState.recentlyConnectedLedgerDevice,
                        uiMessage = delegateState.uiMessage
                    )
                }
            }
        }
    }

    fun onSendAddLedgerRequest() {
        addLedgerDeviceDelegate.onSendAddLedgerRequest()
    }

    fun onConfirmLedgerName(name: String) {
        addLedgerDeviceDelegate.onConfirmLedgerName(name)
        _state.update { it.copy(showContent = LedgerHardwareWalletsUiState.ShowContent.Details) }
    }

    fun onMessageShown() {
        _state.update { it.copy(uiMessage = null) }
    }

    fun onAddLedgerDeviceClick() {
        viewModelScope.launch {
            if (getProfileUseCase.p2pLinks.first().isEmpty()) {
                _state.update { it.copy(showContent = LedgerHardwareWalletsUiState.ShowContent.LinkNewConnector) }
            } else {
                _state.update { it.copy(showContent = LedgerHardwareWalletsUiState.ShowContent.AddLedger) }
            }
        }
    }

    fun onCloseClick() {
        _state.update {
            it.copy(showContent = LedgerHardwareWalletsUiState.ShowContent.Details)
        }
    }

    fun onLinkConnectorClick() {
        _state.update {
            it.copy(showContent = LedgerHardwareWalletsUiState.ShowContent.AddLinkConnector)
        }
    }

    fun onNewConnectorCloseClick() {
        _state.update {
            it.copy(showContent = LedgerHardwareWalletsUiState.ShowContent.Details)
        }
    }
}

data class LedgerHardwareWalletsUiState(
    val loading: Boolean = false,
    val showContent: ShowContent = ShowContent.Details,
    val ledgerDevices: ImmutableList<LedgerHardwareWalletFactorSource> = persistentListOf(),
    val addLedgerSheetState: AddLedgerSheetState = AddLedgerSheetState.AddLedgerDevice,
    val waitingForLedgerResponse: Boolean = false,
    val recentlyConnectedLedgerDevice: LedgerDeviceUiModel? = null,
    val uiMessage: UiMessage? = null
) : UiState {

    enum class ShowContent {
        Details, AddLedger, LinkNewConnector, AddLinkConnector
    }
}
