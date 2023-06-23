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
import rdx.works.profile.data.model.factorsources.FactorSourceKind
import rdx.works.profile.data.model.factorsources.LedgerHardwareWalletFactorSource
import rdx.works.profile.domain.AddLedgerFactorSourceResult
import rdx.works.profile.domain.AddLedgerFactorSourceUseCase
import rdx.works.profile.domain.GetProfileUseCase
import rdx.works.profile.domain.factorSourceById
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
            combine(getProfileUseCase.ledgerFactorSources, getProfileUseCase.p2pLinks) { ledgerFactorSources, p2pLinks ->
                ledgerFactorSources to p2pLinks.isNotEmpty()
            }.collect { factorSourcesToP2pLinksExist ->
                _state.update { state ->
                    state.copy(
                        ledgerFactorSources = factorSourcesToP2pLinksExist.first.toPersistentList(),
                        hasP2pLinks = factorSourcesToP2pLinksExist.second,
                        selectedFactorSourceID = state.selectedFactorSourceID
                            ?: factorSourcesToP2pLinksExist.first.firstOrNull()?.id,
                        addLedgerSheetState = if (factorSourcesToP2pLinksExist.second) {
                            AddLedgerSheetState.Connect
                        } else {
                            AddLedgerSheetState.LinkConnector
                        }
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
            state.copy(selectedFactorSourceID = ledgerFactorSource.id)
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
                        body = FactorSource.HexCoded32Bytes(deviceInfoResponse.deviceId)
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
                        selectedFactorSourceID = ledgerAddResult.ledgerFactorSource.id,
                        addLedgerSheetState = AddLedgerSheetState.Connect,
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

    data class CreateLedgerDelegateState(
        val loading: Boolean = false,
        val ledgerFactorSources: ImmutableList<LedgerHardwareWalletFactorSource> = persistentListOf(),
        val selectedFactorSourceID: FactorSource.FactorSourceID.FromHash? = null,
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
