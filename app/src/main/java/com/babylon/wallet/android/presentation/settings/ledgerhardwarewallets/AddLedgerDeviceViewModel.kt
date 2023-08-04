package com.babylon.wallet.android.presentation.settings.ledgerhardwarewallets

import androidx.lifecycle.viewModelScope
import com.babylon.wallet.android.data.dapp.LedgerMessenger
import com.babylon.wallet.android.domain.model.toProfileLedgerDeviceModel
import com.babylon.wallet.android.presentation.common.StateViewModel
import com.babylon.wallet.android.presentation.common.UiMessage
import com.babylon.wallet.android.presentation.common.UiState
import com.babylon.wallet.android.presentation.model.LedgerDeviceUiModel
import dagger.hilt.android.lifecycle.HiltViewModel
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
import javax.inject.Inject

@HiltViewModel
class AddLedgerDeviceViewModel @Inject constructor(
    private val getProfileUseCase: GetProfileUseCase,
    private val ledgerMessenger: LedgerMessenger,
    private val addLedgerFactorSourceUseCase: AddLedgerFactorSourceUseCase,
) : StateViewModel<AddLedgerDeviceUiState>() {

    override fun initialState() = AddLedgerDeviceUiState.init

    fun onSendAddLedgerRequestClick() {
        viewModelScope.launch {
            _state.update {
                it.copy(isLoading = true)
            }
            val result = ledgerMessenger.sendDeviceInfoRequest(interactionId = UUIDGenerator.uuid().toString())

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
                            isLoading = false,
                            showContent = AddLedgerDeviceUiState.ShowContent.NameLedgerDevice,
                            newConnectedLedgerDevice = LedgerDeviceUiModel(
                                id = deviceInfoResponse.deviceId,
                                model = deviceInfoResponse.model
                            )
                        )
                    }
                } else {
                    _state.update { state ->
                        existingLedgerFactorSource as LedgerHardwareWalletFactorSource
                        state.copy(
                            isLoading = false,
                            showContent = AddLedgerDeviceUiState.ShowContent.AddLedgerDeviceInfo,
                            newConnectedLedgerDevice = LedgerDeviceUiModel(
                                id = deviceInfoResponse.deviceId,
                                model = deviceInfoResponse.model,
                                name = existingLedgerFactorSource.hint.name
                            ),
                            uiMessage = UiMessage.InfoMessage.LedgerAlreadyExist(
                                label = existingLedgerFactorSource.hint.name
                            )
                        )
                    }
                }
            }
            result.onFailure { error ->
                _state.update { state ->
                    state.copy(
                        uiMessage = UiMessage.ErrorMessage.from(error),
                        isLoading = false
                    )
                }
            }
        }
    }

    suspend fun onConfirmLedgerNameClick(name: String) {
        _state.update { state ->
            state.copy(newConnectedLedgerDevice = state.newConnectedLedgerDevice?.copy(name = name))
        }
        addLedgerDeviceInProfile()
    }

    fun initState() {
        _state.value = AddLedgerDeviceUiState.init
    }

    fun onMessageShown() {
        _state.update { it.copy(uiMessage = null) }
    }

    private suspend fun addLedgerDeviceInProfile() {
        state.value.newConnectedLedgerDevice?.let { ledgerDeviceUiModel ->
            val result = addLedgerFactorSourceUseCase(
                ledgerId = ledgerDeviceUiModel.id,
                model = ledgerDeviceUiModel.model.toProfileLedgerDeviceModel(),
                name = ledgerDeviceUiModel.name
            )
            val message: UiMessage? = when (result) {
                is AddLedgerFactorSourceResult.AlreadyExist -> UiMessage.InfoMessage.LedgerAlreadyExist(
                    label = result.ledgerFactorSource.hint.name
                )

                else -> null
            }
            _state.update { state ->
                state.copy(uiMessage = message)
            }
            initState()
        }
    }
}

data class AddLedgerDeviceUiState(
    val isLoading: Boolean,
    val showContent: ShowContent,
    val newConnectedLedgerDevice: LedgerDeviceUiModel?,
    val uiMessage: UiMessage?
) : UiState {

    enum class ShowContent {
        AddLedgerDeviceInfo, NameLedgerDevice
    }

    companion object {
        val init = AddLedgerDeviceUiState(
            isLoading = false,
            showContent = ShowContent.AddLedgerDeviceInfo,
            newConnectedLedgerDevice = null,
            uiMessage = null
        )
    }
}
