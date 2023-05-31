package com.babylon.wallet.android.presentation.createaccount.withledger

import com.babylon.wallet.android.data.dapp.LedgerMessenger
import com.babylon.wallet.android.domain.model.toProfileLedgerDeviceModel
import com.babylon.wallet.android.presentation.common.Stateful
import com.babylon.wallet.android.presentation.common.UiMessage
import com.babylon.wallet.android.presentation.common.UiState
import com.babylon.wallet.android.presentation.model.AddLedgerSheetState
import com.babylon.wallet.android.presentation.model.LedgerDeviceUiModel
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import rdx.works.core.UUIDGenerator
import rdx.works.profile.data.model.factorsources.FactorSource
import rdx.works.profile.domain.AddLedgerFactorSourceUseCase
import rdx.works.profile.domain.GetProfileUseCase
import rdx.works.profile.domain.ledgerFactorSources
import rdx.works.profile.domain.p2pLinks

class CreateLedgerDelegate(
    private val getProfileUseCase: GetProfileUseCase,
    private val ledgerMessenger: LedgerMessenger,
    private val addLedgerFactorSourceUseCase: AddLedgerFactorSourceUseCase,
    private val scope: CoroutineScope
) : Stateful<CreateLedgerDelegate.CreateLedgerDelegateState>() {

    init {
        scope.launch {
            combine(getProfileUseCase.ledgerFactorSources, getProfileUseCase.p2pLinks) { factorSources, p2pLinks ->
                factorSources to p2pLinks.isNotEmpty()
            }.collect { factorSourcesToP2pLinksExist ->
                _state.update { state ->
                    state.copy(
                        ledgerFactorSources = factorSourcesToP2pLinksExist.first.toPersistentList(),
                        hasP2pLinks = factorSourcesToP2pLinksExist.second,
                        selectedFactorSourceID = state.selectedFactorSourceID ?: factorSourcesToP2pLinksExist.first.firstOrNull()?.id
                    )
                }
            }
        }
    }

    fun onSkipLedgerName() {
        addLedgerFactorSource()
    }

    fun onLedgerFactorSourceSelected(ledgerFactorSource: FactorSource) {
        _state.update { state ->
            state.copy(selectedFactorSourceID = ledgerFactorSource.id)
        }
    }

    fun onSendAddLedgerRequest() {
        scope.launch {
            _state.update { it.copy(waitingForLedgerResponse = true) }
            val result = ledgerMessenger.sendDeviceInfoRequest(UUIDGenerator.uuid().toString())
            result.onSuccess { response ->
                _state.update { state ->
                    state.copy(
                        addLedgerSheetState = AddLedgerSheetState.InputLedgerName,
                        waitingForLedgerResponse = false,
                        recentlyConnectedLedgerDevice = LedgerDeviceUiModel(response.deviceId, response.model)
                    )
                }
            }
            result.onFailure { error ->
                _state.update { state -> state.copy(uiMessage = UiMessage.ErrorMessage(error), waitingForLedgerResponse = false) }
            }
        }
    }

    private fun addLedgerFactorSource() {
        scope.launch {
            state.value.recentlyConnectedLedgerDevice?.let { ledger ->
                val ledgerFactorSourceId = addLedgerFactorSourceUseCase(
                    id = FactorSource.ID(ledger.id),
                    model = ledger.model.toProfileLedgerDeviceModel(),
                    name = ledger.name
                )
                _state.update { state ->
                    state.copy(selectedFactorSourceID = ledgerFactorSourceId, addLedgerSheetState = AddLedgerSheetState.Connect)
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

    data class CreateLedgerDelegateState(
        val loading: Boolean = false,
        val ledgerFactorSources: ImmutableList<FactorSource> = persistentListOf(),
        val selectedFactorSourceID: FactorSource.ID? = null,
        val hasP2pLinks: Boolean = false,
        val addLedgerSheetState: AddLedgerSheetState = AddLedgerSheetState.Connect,
        val waitingForLedgerResponse: Boolean = false,
        val recentlyConnectedLedgerDevice: LedgerDeviceUiModel? = null,
        val uiMessage: UiMessage? = null
    ) : UiState

    override fun initialState(): CreateLedgerDelegateState {
        return CreateLedgerDelegateState()
    }
}
