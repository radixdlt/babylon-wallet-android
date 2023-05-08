package com.babylon.wallet.android.presentation.createaccount.withledger

import androidx.lifecycle.viewModelScope
import com.babylon.wallet.android.data.dapp.LedgerMessenger
import com.babylon.wallet.android.data.dapp.model.DerivePublicKeyRequest
import com.babylon.wallet.android.domain.model.toProfileLedgerDeviceModel
import com.babylon.wallet.android.presentation.common.OneOffEvent
import com.babylon.wallet.android.presentation.common.OneOffEventHandler
import com.babylon.wallet.android.presentation.common.OneOffEventHandlerImpl
import com.babylon.wallet.android.presentation.common.StateViewModel
import com.babylon.wallet.android.presentation.common.UiState
import com.babylon.wallet.android.presentation.model.AddLedgerSheetState
import com.babylon.wallet.android.presentation.model.LedgerDeviceUiModel
import com.babylon.wallet.android.utils.AppEvent
import com.babylon.wallet.android.utils.AppEventBus
import com.babylon.wallet.android.utils.getLedgerDeviceModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import rdx.works.core.UUIDGenerator
import rdx.works.profile.data.model.factorsources.FactorSource
import rdx.works.profile.domain.AddLedgerFactorSourceUseCase
import rdx.works.profile.domain.GetProfileUseCase
import rdx.works.profile.domain.ledgerFactorSources
import rdx.works.profile.domain.nextDerivationPathForAccountOnCurrentNetwork
import rdx.works.profile.domain.p2pLinks
import javax.inject.Inject

@HiltViewModel
class CreateAccountWithLedgerViewModel @Inject constructor(
    private val getProfileUseCase: GetProfileUseCase,
    private val ledgerMessenger: LedgerMessenger,
    private val addLedgerFactorSourceUseCase: AddLedgerFactorSourceUseCase,
    private val appEventBus: AppEventBus
) : StateViewModel<CreateAccountWithLedgerViewModel.CreateAccountWithLedgerState>(),
    OneOffEventHandler<CreateAccountWithLedgerEvent> by OneOffEventHandlerImpl() {

    init {
        viewModelScope.launch {
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

    override fun initialState(): CreateAccountWithLedgerState = CreateAccountWithLedgerState()

    fun onLedgerFactorSourceSelected(ledgerFactorSource: FactorSource) {
        _state.update { state ->
            state.copy(selectedFactorSourceID = ledgerFactorSource.id)
        }
    }

    fun onSendAddLedgerRequest() {
        viewModelScope.launch {
            _state.update { it.copy(waitingForLedgerResponse = true) }
            ledgerMessenger.sendDeviceInfoRequest(UUIDGenerator.uuid().toString()).collect { response ->
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

    fun onUseLedger() {
        state.value.ledgerFactorSources.firstOrNull { it.id == state.value.selectedFactorSourceID }?.let { ledgerFactorSource ->
            viewModelScope.launch {
                _state.update { it.copy(waitingForLedgerResponse = true) }
                val derivationPath = getProfileUseCase.nextDerivationPathForAccountOnCurrentNetwork(ledgerFactorSource)
                val deviceModel = requireNotNull(ledgerFactorSource.getLedgerDeviceModel())
                ledgerMessenger.sendDeriveCurve25519PublicKeyRequest(
                    UUIDGenerator.uuid().toString(),
                    derivationPath.path,
                    DerivePublicKeyRequest.LedgerDevice(ledgerFactorSource.label, deviceModel, ledgerFactorSource.id.value)
                ).collect { response ->
                    appEventBus.sendEvent(
                        AppEvent.DerivedAccountPublicKeyWithLedger(
                            factorSourceID = ledgerFactorSource.id,
                            derivationPath = derivationPath,
                            derivedPublicKeyHex = response.publicKeyHex
                        )
                    )
                    sendEvent(CreateAccountWithLedgerEvent.DerivedPublicKeyForAccount)
                }
            }
        }
    }

    private fun addLedgerFactorSource() {
        viewModelScope.launch {
            state.value.recentlyConnectedLedgerDevice?.let { ledger ->
                val ledgerFactorSourceId = addLedgerFactorSourceUseCase(
                    id = FactorSource.ID(ledger.id),
                    model = ledger.model.toProfileLedgerDeviceModel(),
                    name = ledger.name
                )
                _state.update { state ->
                    state.copy(selectedFactorSourceID = ledgerFactorSourceId, addLedgerSheetState = AddLedgerSheetState.Initial)
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

    data class CreateAccountWithLedgerState(
        val loading: Boolean = false,
        val ledgerFactorSources: ImmutableList<FactorSource> = persistentListOf(),
        val selectedFactorSourceID: FactorSource.ID? = null,
        val hasP2pLinks: Boolean = false,
        val addLedgerSheetState: AddLedgerSheetState = AddLedgerSheetState.Initial,
        val waitingForLedgerResponse: Boolean = false,
        var recentlyConnectedLedgerDevice: LedgerDeviceUiModel? = null
    ) : UiState
}

internal sealed interface CreateAccountWithLedgerEvent : OneOffEvent {
    object DerivedPublicKeyForAccount : CreateAccountWithLedgerEvent
}
