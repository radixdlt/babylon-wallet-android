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
import rdx.works.profile.domain.AddLedgerFactorSourceUseCase
import rdx.works.profile.domain.GetProfileUseCase
import rdx.works.profile.domain.factorSourceById
import rdx.works.profile.domain.p2pLinks

class UseLedgerDelegate(
    private val getProfileUseCase: GetProfileUseCase,
    private val ledgerMessenger: LedgerMessenger,
    private val addLedgerFactorSourceUseCase: AddLedgerFactorSourceUseCase,
    private val scope: CoroutineScope,
    private val onUseLedger: suspend (LedgerHardwareWalletFactorSource) -> Unit
) : Stateful<UseLedgerDelegate.UseLedgerDelegateState>() {

    init {
        scope.launch {
            getProfileUseCase.p2pLinks.collect { p2pLinks ->
                _state.update { state ->
                    state.copy(
                        hasP2pLinks = p2pLinks.isNotEmpty(),
                        addLedgerSheetState = if (p2pLinks.isNotEmpty()) {
                            AddLedgerSheetState.Connect
                        } else {
                            AddLedgerSheetState.LinkConnector
                        }
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
                            addLedgerSheetState = AddLedgerSheetState.Connect,
                            waitingForLedgerResponse = false,
                            usedLedgerFactorSources = (
                                state.usedLedgerFactorSources +
                                    existingLedgerFactorSource
                                ).distinct().toPersistentList()
                        )
                    }
                    onUseLedger(existingLedgerFactorSource as LedgerHardwareWalletFactorSource)
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
                val result = addLedgerFactorSourceUseCase(
                    ledgerId = ledgerDeviceUiModel.id,
                    model = ledgerDeviceUiModel.model.toProfileLedgerDeviceModel(),
                    name = ledgerDeviceUiModel.name
                )
                _state.update { state ->
                    state.copy(
                        addLedgerSheetState = AddLedgerSheetState.Connect,
                        usedLedgerFactorSources = (state.usedLedgerFactorSources + result.ledgerFactorSource).toPersistentList()
                    )
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
        val usedLedgerFactorSources: ImmutableList<LedgerHardwareWalletFactorSource> = persistentListOf(),
        val hasP2pLinks: Boolean = false,
        val addLedgerSheetState: AddLedgerSheetState = AddLedgerSheetState.Connect,
        val waitingForLedgerResponse: Boolean = false,
        val recentlyConnectedLedgerDevice: LedgerDeviceUiModel? = null,
        val uiMessage: UiMessage? = null
    ) : UiState

    override fun initialState(): UseLedgerDelegateState {
        return UseLedgerDelegateState()
    }
}
