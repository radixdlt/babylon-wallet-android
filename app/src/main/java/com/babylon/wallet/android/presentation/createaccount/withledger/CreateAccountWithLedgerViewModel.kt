package com.babylon.wallet.android.presentation.createaccount.withledger

import androidx.lifecycle.viewModelScope
import com.babylon.wallet.android.data.dapp.LedgerMessenger
import com.babylon.wallet.android.data.dapp.model.Curve
import com.babylon.wallet.android.data.dapp.model.DerivePublicKeyRequest
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
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import rdx.works.core.UUIDGenerator
import rdx.works.profile.data.model.factorsources.FactorSource
import rdx.works.profile.domain.AddLedgerFactorSourceUseCase
import rdx.works.profile.domain.GetProfileUseCase
import rdx.works.profile.domain.nextDerivationPathForAccountOnCurrentNetwork
import javax.inject.Inject

@HiltViewModel
class CreateAccountWithLedgerViewModel @Inject constructor(
    private val getProfileUseCase: GetProfileUseCase,
    private val ledgerMessenger: LedgerMessenger,
    addLedgerFactorSourceUseCase: AddLedgerFactorSourceUseCase,
    private val appEventBus: AppEventBus
) : StateViewModel<CreateAccountWithLedgerViewModel.CreateAccountWithLedgerState>(),
    OneOffEventHandler<CreateAccountWithLedgerEvent> by OneOffEventHandlerImpl() {

    private val createLedgerDelegate = CreateLedgerDelegate(
        getProfileUseCase = getProfileUseCase,
        ledgerMessenger = ledgerMessenger,
        addLedgerFactorSourceUseCase = addLedgerFactorSourceUseCase,
        scope = viewModelScope
    )

    init {
        viewModelScope.launch {
            createLedgerDelegate.state.collect { delegateState ->
                _state.update { uiState ->
                    uiState.copy(
                        loading = delegateState.loading,
                        selectedFactorSourceID = delegateState.selectedFactorSourceID,
                        addLedgerSheetState = delegateState.addLedgerSheetState,
                        ledgerFactorSources = delegateState.ledgerFactorSources,
                        waitingForLedgerResponse = delegateState.waitingForLedgerResponse,
                        recentlyConnectedLedgerDevice = delegateState.recentlyConnectedLedgerDevice,
                        hasP2pLinks = delegateState.hasP2pLinks
                    )
                }
            }
        }
    }

    override fun initialState(): CreateAccountWithLedgerState = CreateAccountWithLedgerState()

    fun onLedgerFactorSourceSelected(ledgerFactorSource: FactorSource) {
        createLedgerDelegate.onLedgerFactorSourceSelected(ledgerFactorSource)
    }

    fun onSendAddLedgerRequest() {
        createLedgerDelegate.onSendAddLedgerRequest()
    }

    fun onSkipLedgerName() {
        createLedgerDelegate.onSkipLedgerName()
    }

    fun onUseLedger() {
        state.value.ledgerFactorSources.firstOrNull { it.id == state.value.selectedFactorSourceID }?.let { ledgerFactorSource ->
            viewModelScope.launch {
                _state.update { it.copy(waitingForLedgerResponse = true) }
                val derivationPath = getProfileUseCase.nextDerivationPathForAccountOnCurrentNetwork(ledgerFactorSource)
                val deviceModel = requireNotNull(ledgerFactorSource.getLedgerDeviceModel())
                val result = ledgerMessenger.sendDerivePublicKeyRequest(
                    UUIDGenerator.uuid().toString(),
                    listOf(DerivePublicKeyRequest.KeyParameters(Curve.Curve25519, derivationPath.path)),
                    DerivePublicKeyRequest.LedgerDevice(ledgerFactorSource.label, deviceModel, ledgerFactorSource.id.value)
                )
                result.onSuccess { response ->
                    appEventBus.sendEvent(
                        AppEvent.DerivedAccountPublicKeyWithLedger(
                            factorSourceID = ledgerFactorSource.id,
                            derivationPath = derivationPath,
                            derivedPublicKeyHex = response.publicKeysHex.first().publicKeyHex
                        )
                    )
                    sendEvent(CreateAccountWithLedgerEvent.DerivedPublicKeyForAccount)
                }
            }
        }
    }

    fun onConfirmLedgerName(name: String) {
        createLedgerDelegate.onConfirmLedgerName(name)
    }

    data class CreateAccountWithLedgerState(
        val loading: Boolean = false,
        val ledgerFactorSources: ImmutableList<FactorSource> = persistentListOf(),
        val selectedFactorSourceID: FactorSource.ID? = null,
        val hasP2pLinks: Boolean = false,
        val addLedgerSheetState: AddLedgerSheetState = AddLedgerSheetState.Connect,
        val waitingForLedgerResponse: Boolean = false,
        var recentlyConnectedLedgerDevice: LedgerDeviceUiModel? = null
    ) : UiState
}

internal sealed interface CreateAccountWithLedgerEvent : OneOffEvent {
    object DerivedPublicKeyForAccount : CreateAccountWithLedgerEvent
}
