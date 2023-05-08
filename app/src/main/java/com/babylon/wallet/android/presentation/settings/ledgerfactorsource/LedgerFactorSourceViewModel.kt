package com.babylon.wallet.android.presentation.settings.ledgerfactorsource

import androidx.lifecycle.viewModelScope
import com.babylon.wallet.android.data.dapp.LedgerMessenger
import com.babylon.wallet.android.domain.model.toProfileLedgerDeviceModel
import com.babylon.wallet.android.presentation.common.StateViewModel
import com.babylon.wallet.android.presentation.common.UiState
import com.babylon.wallet.android.presentation.model.AddLedgerSheetState
import com.babylon.wallet.android.presentation.model.LedgerDeviceUiModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.flow.cancellable
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import rdx.works.core.UUIDGenerator
import rdx.works.profile.data.model.factorsources.FactorSource
import rdx.works.profile.domain.AddLedgerFactorSourceUseCase
import rdx.works.profile.domain.GetProfileUseCase
import rdx.works.profile.domain.ledgerFactorSources
import rdx.works.profile.domain.p2pLinks
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class LedgerFactorSourcesViewModel @Inject constructor(
    private val getProfileUseCase: GetProfileUseCase,
    private val ledgerMessenger: LedgerMessenger,
    private val addLedgerFactorSourceUseCase: AddLedgerFactorSourceUseCase,
) : StateViewModel<LedgerFactorSourcesUiState>() {

    override fun initialState(): LedgerFactorSourcesUiState = LedgerFactorSourcesUiState()

    init {
        viewModelScope.launch {
            combine(getProfileUseCase.ledgerFactorSources, getProfileUseCase.p2pLinks) { factorSources, p2pLinks ->
                factorSources to p2pLinks.isNotEmpty()
            }.collect { factorSourcesToP2pLinksExist ->
                _state.update { state ->
                    state.copy(
                        ledgerFactorSources = factorSourcesToP2pLinksExist.first.toPersistentList(),
                        hasP2pLinks = factorSourcesToP2pLinksExist.second
                    )
                }
            }
        }
    }

    fun onShowDeleteLedgerDialog(factorSourceID: FactorSource.ID) {
        viewModelScope.launch {
            _state.update { it.copy(deleteLedgerDialogState = DeleteLedgerDialogState.Shown(factorSourceID = factorSourceID)) }
        }
    }

    fun closeDeleteLedgerDialog(factorSourceId: FactorSource.ID?) {
        viewModelScope.launch {
            if (factorSourceId != null) {
                // TODO delete factor source
                Timber.d("Factor source delete No-op")
            }
            _state.update { it.copy(deleteLedgerDialogState = DeleteLedgerDialogState.None) }
        }
    }

    fun onSendAddLedgerRequest() {
        viewModelScope.launch {
            _state.update { it.copy(waitingForLedgerResponse = true) }
            ledgerMessenger.sendDeviceInfoRequest(UUIDGenerator.uuid().toString()).cancellable().collect { response ->
                _state.update { state ->
                    state.copy(
                        addLedgerSheetState = AddLedgerSheetState.InputLedgerName,
                        waitingForLedgerResponse = false,
                        recentlyConnectedLedgerDevice = LedgerDeviceUiModel(response.deviceId, response.model)
                    )
                }
            }
        }
    }

    fun onSkipLedgerName() {
        addLedgerFactorSource()
    }

    private fun addLedgerFactorSource() {
        viewModelScope.launch {
            state.value.recentlyConnectedLedgerDevice?.let { ledger ->
                addLedgerFactorSourceUseCase(
                    id = FactorSource.ID(ledger.id),
                    model = ledger.model.toProfileLedgerDeviceModel(),
                    name = ledger.name
                )
                _state.update { state ->
                    state.copy(addLedgerSheetState = AddLedgerSheetState.Initial)
                }
            }
        }
    }

    fun onConfirmLedgerName(name: String) {
        _state.update { state ->
            state.copy(recentlyConnectedLedgerDevice = state.recentlyConnectedLedgerDevice?.copy(name = name))
        }
        addLedgerFactorSource()
    }
}

data class LedgerFactorSourcesUiState(
    val ledgerFactorSources: ImmutableList<FactorSource> = persistentListOf(),
    val deleteLedgerDialogState: DeleteLedgerDialogState = DeleteLedgerDialogState.None,
    val loading: Boolean = false,
    val hasP2pLinks: Boolean = false,
    val addLedgerSheetState: AddLedgerSheetState = AddLedgerSheetState.Initial,
    val waitingForLedgerResponse: Boolean = false,
    var recentlyConnectedLedgerDevice: LedgerDeviceUiModel? = null
) : UiState

sealed interface DeleteLedgerDialogState {
    object None : DeleteLedgerDialogState
    data class Shown(val factorSourceID: FactorSource.ID) : DeleteLedgerDialogState
}
