package com.babylon.wallet.android.presentation.settings.accountsecurity.ledgerhardwarewallets

import androidx.lifecycle.viewModelScope
import com.babylon.wallet.android.presentation.common.StateViewModel
import com.babylon.wallet.android.presentation.common.UiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import rdx.works.profile.data.model.factorsources.LedgerHardwareWalletFactorSource
import rdx.works.profile.domain.GetProfileUseCase
import rdx.works.profile.domain.ledgerFactorSources
import rdx.works.profile.domain.p2pLinks
import javax.inject.Inject

@HiltViewModel
class LedgerHardwareWalletsViewModel @Inject constructor(
    private val getProfileUseCase: GetProfileUseCase
) : StateViewModel<LedgerHardwareWalletsUiState>() {

    override fun initialState(): LedgerHardwareWalletsUiState = LedgerHardwareWalletsUiState()

    init {
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
    val ledgerDevices: ImmutableList<LedgerHardwareWalletFactorSource> = persistentListOf()
) : UiState {

    enum class ShowContent {
        Details, AddLedger, LinkNewConnector, AddLinkConnector
    }
}
