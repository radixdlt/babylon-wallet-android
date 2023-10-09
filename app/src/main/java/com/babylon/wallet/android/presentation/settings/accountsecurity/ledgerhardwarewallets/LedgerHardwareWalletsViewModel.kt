package com.babylon.wallet.android.presentation.settings.accountsecurity.ledgerhardwarewallets

import androidx.lifecycle.viewModelScope
import com.babylon.wallet.android.data.dapp.LedgerMessenger
import com.babylon.wallet.android.presentation.common.StateViewModel
import com.babylon.wallet.android.presentation.common.UiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import rdx.works.profile.data.model.factorsources.LedgerHardwareWalletFactorSource
import rdx.works.profile.domain.GetProfileUseCase
import rdx.works.profile.domain.ledgerFactorSources
import rdx.works.profile.domain.p2pLinks
import javax.inject.Inject

@HiltViewModel
class LedgerHardwareWalletsViewModel @Inject constructor(
    private val getProfileUseCase: GetProfileUseCase,
    private val ledgerMessenger: LedgerMessenger
) : StateViewModel<LedgerHardwareWalletsUiState>() {

    override fun initialState(): LedgerHardwareWalletsUiState = LedgerHardwareWalletsUiState()

    init {
        viewModelScope.launch {
            ledgerMessenger.isConnected.collect { connected ->
                _state.update { it.copy(isLinkConnectionEstablished = connected) }
            }
        }
        viewModelScope.launch {
            getProfileUseCase.ledgerFactorSources.collect { ledgerDevices ->
                _state.update { uiState ->
                    uiState.copy(ledgerDevices = ledgerDevices.toPersistentList())
                }
            }
        }
    }

    fun onAddLedgerDeviceClick() {
        viewModelScope.launch {
            if (getProfileUseCase.p2pLinks.first().isEmpty()) {
                _state.update { it.copy(showContent = LedgerHardwareWalletsUiState.ShowContent.LinkNewConnector(false)) }
            } else {
                if (!state.value.isLinkConnectionEstablished) {
                    _state.update {
                        it.copy(
                            showLinkConnectorPromptState = ShowLinkConnectorPromptState.Show(
                                ShowLinkConnectorPromptState.Source.UseLedger
                            )
                        )
                    }
                } else {
                    _state.update { it.copy(showContent = LedgerHardwareWalletsUiState.ShowContent.AddLedger) }
                }
            }
        }
    }

    fun dismissConnectorPrompt(linkConnector: Boolean, source: ShowLinkConnectorPromptState.Source) {
        _state.update {
            it.copy(
                showContent = if (linkConnector) {
                    LedgerHardwareWalletsUiState.ShowContent.LinkNewConnector(
                        source == ShowLinkConnectorPromptState.Source.AddLedgerDevice
                    )
                } else {
                    it.showContent
                },
                showLinkConnectorPromptState = ShowLinkConnectorPromptState.None
            )
        }
    }

    fun disableAddLedgerButtonUntilConnectionIsEstablished() {
        _state.update {
            it.copy(
                showContent = LedgerHardwareWalletsUiState.ShowContent.Details,
                addLedgerEnabled = false
            )
        }
        viewModelScope.launch {
            ledgerMessenger.isConnected.filter { it }.firstOrNull()?.let {
                _state.update { state ->
                    state.copy(addLedgerEnabled = true)
                }
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
            it.copy(showContent = LedgerHardwareWalletsUiState.ShowContent.AddLinkConnector(false))
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
    val showLinkConnectorPromptState: ShowLinkConnectorPromptState = ShowLinkConnectorPromptState.None,
    val isLinkConnectionEstablished: Boolean = false,
    val addLedgerEnabled: Boolean = true
) : UiState {

    sealed interface ShowContent {
        data object Details : ShowContent
        data object AddLedger : ShowContent
        data class LinkNewConnector(val addDeviceAfterLinking: Boolean = true) : ShowContent
        data class AddLinkConnector(val addDeviceAfterLinking: Boolean = true) : ShowContent
    }
}
