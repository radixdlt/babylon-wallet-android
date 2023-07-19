package com.babylon.wallet.android.presentation.settings.ledgerfactorsource

import androidx.lifecycle.viewModelScope
import com.babylon.wallet.android.data.dapp.LedgerMessenger
import com.babylon.wallet.android.presentation.common.StateViewModel
import com.babylon.wallet.android.presentation.common.UiMessage
import com.babylon.wallet.android.presentation.common.UiState
import com.babylon.wallet.android.presentation.createaccount.withledger.CreateLedgerDelegate
import com.babylon.wallet.android.presentation.model.AddLedgerSheetState
import com.babylon.wallet.android.presentation.model.LedgerDeviceUiModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import rdx.works.profile.data.model.factorsources.LedgerHardwareWalletFactorSource
import rdx.works.profile.domain.AddLedgerFactorSourceUseCase
import rdx.works.profile.domain.GetProfileUseCase
import javax.inject.Inject

@HiltViewModel
class LedgerFactorSourcesViewModel @Inject constructor(
    getProfileUseCase: GetProfileUseCase,
    ledgerMessenger: LedgerMessenger,
    addLedgerFactorSourceUseCase: AddLedgerFactorSourceUseCase,
) : StateViewModel<LedgerFactorSourcesUiState>() {

    private val createLedgerDelegate = CreateLedgerDelegate(
        getProfileUseCase = getProfileUseCase,
        ledgerMessenger = ledgerMessenger,
        addLedgerFactorSourceUseCase = addLedgerFactorSourceUseCase,
        scope = viewModelScope
    )

    override fun initialState(): LedgerFactorSourcesUiState = LedgerFactorSourcesUiState()

    init {
        viewModelScope.launch {
            createLedgerDelegate.state.collect { delegateState ->
                _state.update { uiState ->
                    uiState.copy(
                        loading = delegateState.loading,
                        addLedgerSheetState = delegateState.addLedgerSheetState,
                        ledgerFactorSources = delegateState.ledgerFactorSources.toPersistentList(),
                        waitingForLedgerResponse = delegateState.waitingForLedgerResponse,
                        recentlyConnectedLedgerDevice = delegateState.recentlyConnectedLedgerDevice,
                        hasP2pLinks = delegateState.hasP2pLinks,
                        uiMessage = delegateState.uiMessage
                    )
                }
            }
        }
    }

    fun onSendAddLedgerRequest() {
        createLedgerDelegate.onSendAddLedgerRequest()
    }

    fun onConfirmLedgerName(name: String) {
        createLedgerDelegate.onConfirmLedgerName(name)
        _state.update { it.copy(mode = LedgerFactorSourcesMode.Details) }
    }

    fun onMessageShown() {
        _state.update { it.copy(uiMessage = null) }
    }

    fun onAddLedgerClick() {
        _state.update { it.copy(mode = LedgerFactorSourcesMode.AddLedger) }
    }

    fun onCloseAddLedgerClick() {
        _state.update { it.copy(mode = LedgerFactorSourcesMode.Details) }
    }
}

enum class LedgerFactorSourcesMode {
    Details, AddLedger
}

data class LedgerFactorSourcesUiState(
    val ledgerFactorSources: ImmutableList<LedgerHardwareWalletFactorSource> = persistentListOf(),
    val loading: Boolean = false,
    val hasP2pLinks: Boolean = false,
    val addLedgerSheetState: AddLedgerSheetState = AddLedgerSheetState.Connect,
    val waitingForLedgerResponse: Boolean = false,
    val recentlyConnectedLedgerDevice: LedgerDeviceUiModel? = null,
    val mode: LedgerFactorSourcesMode = LedgerFactorSourcesMode.Details,
    val uiMessage: UiMessage? = null
) : UiState
