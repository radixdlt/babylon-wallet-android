package com.babylon.wallet.android.presentation.settings.securitycenter.ledgerhardwarewallets

import androidx.lifecycle.viewModelScope
import com.babylon.wallet.android.data.dapp.LedgerMessenger
import com.babylon.wallet.android.presentation.common.StateViewModel
import com.babylon.wallet.android.presentation.common.UiState
import com.radixdlt.sargon.FactorSource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.flow.dropWhile
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import rdx.works.core.sargon.ledgerFactorSources
import rdx.works.profile.domain.GetProfileUseCase
import javax.inject.Inject

@HiltViewModel
class LedgerHardwareWalletsViewModel @Inject constructor(
    private val getProfileUseCase: GetProfileUseCase,
    private val ledgerMessenger: LedgerMessenger
) : StateViewModel<LedgerHardwareWalletsUiState>() {

    override fun initialState(): LedgerHardwareWalletsUiState = LedgerHardwareWalletsUiState()

    init {
        viewModelScope.launch {
            getProfileUseCase.flow.map { it.ledgerFactorSources }.collect { ledgerDevices ->
                _state.update { uiState ->
                    uiState.copy(ledgerDevices = ledgerDevices.toPersistentList())
                }
            }
        }
    }

    fun onAddLedgerDeviceClick() {
        viewModelScope.launch {
            val hasAtLeastOneLinkedConnector = getProfileUseCase().appPreferences.p2pLinks.isNotEmpty()
            if (hasAtLeastOneLinkedConnector) {
                _state.update {
                    it.copy(showContent = LedgerHardwareWalletsUiState.ShowContent.AddLedger)
                }
            } else {
                _state.update {
                    it.copy(
                        showLinkConnectorPromptState = ShowLinkConnectorPromptState.Show(
                            source = ShowLinkConnectorPromptState.Source.UseLedger
                        )
                    )
                }
            }
        }
    }

    fun dismissConnectorPrompt(linkConnector: Boolean) {
        _state.update {
            it.copy(
                showContent = if (linkConnector) {
                    LedgerHardwareWalletsUiState.ShowContent.LinkNewConnector
                } else {
                    it.showContent
                },
                showLinkConnectorPromptState = ShowLinkConnectorPromptState.None
            )
        }
    }

    fun disableAddLedgerButtonUntilConnectionIsEstablished() {
        _state.update {
            it.copy(showContent = LedgerHardwareWalletsUiState.ShowContent.Details)
        }
        ledgerMessenger.isAnyLinkedConnectorConnected
            .dropWhile { isConnected ->
                _state.update { state ->
                    state.copy(isNewLinkedConnectorConnected = isConnected)
                }
                isConnected.not() // continue while isConnected is not true
            }
            .launchIn(viewModelScope)
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
    val ledgerDevices: ImmutableList<FactorSource.Ledger> = persistentListOf(),
    val showLinkConnectorPromptState: ShowLinkConnectorPromptState = ShowLinkConnectorPromptState.None,
    val isNewLinkedConnectorConnected: Boolean = true
) : UiState {

    sealed interface ShowContent {
        data object Details : ShowContent
        data object AddLedger : ShowContent
        data object LinkNewConnector : ShowContent
        data object AddLinkConnector : ShowContent
    }
}
