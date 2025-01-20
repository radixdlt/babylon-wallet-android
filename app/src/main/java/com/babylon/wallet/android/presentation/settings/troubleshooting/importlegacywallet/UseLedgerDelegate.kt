package com.babylon.wallet.android.presentation.settings.troubleshooting.importlegacywallet

import com.babylon.wallet.android.data.dapp.LedgerMessenger
import com.babylon.wallet.android.presentation.common.Stateful
import com.babylon.wallet.android.presentation.common.UiMessage
import com.babylon.wallet.android.presentation.common.UiState
import com.babylon.wallet.android.presentation.model.LedgerDeviceUiModel
import com.babylon.wallet.android.presentation.settings.securitycenter.securityfactors.ledgerdevice.AddLedgerDeviceUiState
import com.radixdlt.sargon.FactorSource
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import rdx.works.core.UUIDGenerator
import rdx.works.core.sargon.factorSourceById
import rdx.works.core.sargon.ledgerFactorSources
import rdx.works.profile.domain.AddLedgerFactorSourceUseCase
import rdx.works.profile.domain.GetProfileUseCase

class UseLedgerDelegate(
    private val getProfileUseCase: GetProfileUseCase,
    private val ledgerMessenger: LedgerMessenger,
    private val addLedgerFactorSourceUseCase: AddLedgerFactorSourceUseCase,
    private val scope: CoroutineScope,
    private val onUseLedger: suspend (FactorSource.Ledger) -> Unit
) : Stateful<UseLedgerDelegate.UseLedgerDelegateState>() {

    init {
        scope.launch {
            getProfileUseCase.flow.map { it.ledgerFactorSources }.collect { ledgerDevices ->
                _state.update { state ->
                    state.copy(
                        hasLedgerDevices = ledgerDevices.isNotEmpty(),
                    )
                }
            }
        }
    }

    fun onSendAddLedgerRequest() {
        scope.launch {
            _state.update { it.copy(waitingForLedgerResponse = true) }
            val result = ledgerMessenger.sendDeviceInfoRequest(UUIDGenerator.uuid().toString())
            result.onSuccess { deviceInfoResponse ->
                val existingLedgerFactorSource = getProfileUseCase().factorSourceById(deviceInfoResponse.factorSourceId)
                if (existingLedgerFactorSource == null) {
                    _state.update { state ->
                        state.copy(
                            addLedgerSheetState = AddLedgerDeviceUiState.ShowContent.NameLedgerDevice,
                            waitingForLedgerResponse = false,
                            recentlyConnectedLedgerDevice = LedgerDeviceUiModel(
                                id = deviceInfoResponse.deviceId,
                                model = deviceInfoResponse.model
                            )
                        )
                    }
                } else {
                    _state.update { state ->
                        existingLedgerFactorSource as FactorSource.Ledger
                        state.copy(
                            addLedgerSheetState = AddLedgerDeviceUiState.ShowContent.AddLedgerDeviceInfo,
                            waitingForLedgerResponse = false
                        )
                    }
                    onUseLedger(existingLedgerFactorSource as FactorSource.Ledger)
                }
            }
            result.onFailure { error ->
                _state.update { state ->
                    state.copy(
                        uiMessage = UiMessage.ErrorMessage(error),
                        waitingForLedgerResponse = false
                    )
                }
            }
        }
    }

    private fun addLedgerFactorSource() {
        scope.launch {
            state.value.recentlyConnectedLedgerDevice?.let { ledgerDeviceUiModel ->
                val result = addLedgerFactorSourceUseCase(
                    ledgerId = ledgerDeviceUiModel.factorSourceId,
                    model = ledgerDeviceUiModel.model.toProfileLedgerDeviceModel(),
                    name = ledgerDeviceUiModel.name
                )
                _state.update { state ->
                    state.copy(addLedgerSheetState = AddLedgerDeviceUiState.ShowContent.AddLedgerDeviceInfo)
                }
                onUseLedger(result.ledgerFactorSource)
            }
        }
    }

    fun onConfirmLedgerName(name: String) {
        _state.update { state ->
            state.copy(recentlyConnectedLedgerDevice = state.recentlyConnectedLedgerDevice?.copy(name = name))
        }
        addLedgerFactorSource()
    }

    data class UseLedgerDelegateState(
        val loading: Boolean = false,
        val hasLedgerDevices: Boolean = false,
        val addLedgerSheetState: AddLedgerDeviceUiState.ShowContent = AddLedgerDeviceUiState.ShowContent.AddLedgerDeviceInfo,
        val waitingForLedgerResponse: Boolean = false,
        val recentlyConnectedLedgerDevice: LedgerDeviceUiModel? = null,
        val uiMessage: UiMessage? = null
    ) : UiState

    override fun initialState(): UseLedgerDelegateState {
        return UseLedgerDelegateState()
    }
}
