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
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import rdx.works.core.UUIDGenerator
import rdx.works.profile.data.model.factorsources.FactorSource
import rdx.works.profile.data.model.factorsources.FactorSourceKind
import rdx.works.profile.data.model.factorsources.LedgerHardwareWalletFactorSource
import rdx.works.profile.domain.AddLedgerFactorSourceResult
import rdx.works.profile.domain.AddLedgerFactorSourceUseCase
import rdx.works.profile.domain.GetProfileUseCase
import rdx.works.profile.domain.factorSourceById
import rdx.works.profile.domain.ledgerFactorSources

class AddLedgerDeviceDelegate(
    private val getProfileUseCase: GetProfileUseCase,
    private val ledgerMessenger: LedgerMessenger,
    private val addLedgerFactorSourceUseCase: AddLedgerFactorSourceUseCase,
    private val scope: CoroutineScope
) : Stateful<AddLedgerDeviceDelegate.AddLedgerDeviceState>() {

    init {
        scope.launch {
            getProfileUseCase.ledgerFactorSources.collect { ledgerDevices ->
                _state.update { state ->
                    state.copy(
                        ledgerDevices = ledgerDevices.toPersistentList(),
                        selectedLedgerDeviceId = state.selectedLedgerDeviceId ?: ledgerDevices.firstOrNull()?.id,
                    )
                }
            }
        }
    }

    fun onSkipLedgerName() {
        addLedgerFactorSource()
    }

    fun onLedgerFactorSourceSelected(ledgerFactorSource: LedgerHardwareWalletFactorSource) {
        _state.update { state ->
            state.copy(selectedLedgerDeviceId = ledgerFactorSource.id)
        }
    }

    fun onSendAddLedgerRequest() {
        scope.launch {
            _state.update { it.copy(waitingForLedgerResponse = true) }
            val result = ledgerMessenger.sendDeviceInfoRequest(UUIDGenerator.uuid().toString())
            result.onSuccess { deviceInfoResponse ->
                val existingLedgerFactorSource = getProfileUseCase.factorSourceById(
                    FactorSource.FactorSourceID.FromHash(
                        kind = FactorSourceKind.LEDGER_HQ_HARDWARE_WALLET,
                        body = deviceInfoResponse.deviceId
                    )
                )
                if (existingLedgerFactorSource == null) {
                    _state.update { state ->
                        state.copy(
                            addLedgerSheetState = AddLedgerSheetState.InputLedgerName,
                            waitingForLedgerResponse = false,
                            recentlyConnectedLedgerDevice = LedgerDeviceUiModel(
                                id = deviceInfoResponse.deviceId,
                                model = deviceInfoResponse.model
                            )
                        )
                    }
                } else {
                    _state.update { state ->
                        existingLedgerFactorSource as LedgerHardwareWalletFactorSource
                        state.copy(
                            addLedgerSheetState = AddLedgerSheetState.AddLedgerDevice,
                            uiMessage = UiMessage.InfoMessage.LedgerAlreadyExist(existingLedgerFactorSource.hint.name),
                            recentlyConnectedLedgerDevice = LedgerDeviceUiModel(
                                id = deviceInfoResponse.deviceId,
                                model = deviceInfoResponse.model,
                                name = existingLedgerFactorSource.hint.name
                            ),
                            waitingForLedgerResponse = false
                        )
                    }
                }
            }
            result.onFailure { error ->
                _state.update { state ->
                    state.copy(
                        uiMessage = UiMessage.ErrorMessage.from(error),
                        waitingForLedgerResponse = false
                    )
                }
            }
        }
    }

    private fun addLedgerFactorSource() {
        scope.launch {
            state.value.recentlyConnectedLedgerDevice?.let { ledgerDeviceUiModel ->
                val ledgerAddResult = addLedgerFactorSourceUseCase(
                    ledgerId = ledgerDeviceUiModel.id,
                    model = ledgerDeviceUiModel.model.toProfileLedgerDeviceModel(),
                    name = ledgerDeviceUiModel.name
                )
                val message: UiMessage? = when (ledgerAddResult) {
                    is AddLedgerFactorSourceResult.AlreadyExist -> UiMessage.InfoMessage.LedgerAlreadyExist(
                        label = ledgerAddResult.ledgerFactorSource.hint.name
                    )
                    else -> null
                }
                _state.update { state ->
                    state.copy(
                        selectedLedgerDeviceId = ledgerAddResult.ledgerFactorSource.id,
                        addLedgerSheetState = AddLedgerSheetState.AddLedgerDevice,
                        uiMessage = message
                    )
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

    data class AddLedgerDeviceState(
        val loading: Boolean = false,
        val ledgerDevices: ImmutableList<LedgerHardwareWalletFactorSource> = persistentListOf(),
        val selectedLedgerDeviceId: FactorSource.FactorSourceID.FromHash? = null,
        val addLedgerSheetState: AddLedgerSheetState = AddLedgerSheetState.AddLedgerDevice,
        val waitingForLedgerResponse: Boolean = false,
        val recentlyConnectedLedgerDevice: LedgerDeviceUiModel? = null,
        val uiMessage: UiMessage? = null
    ) : UiState

    override fun initialState(): AddLedgerDeviceState {
        return AddLedgerDeviceState()
    }
}
