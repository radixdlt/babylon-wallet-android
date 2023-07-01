package com.babylon.wallet.android.presentation.createaccount.withledger

import androidx.lifecycle.viewModelScope
import com.babylon.wallet.android.data.dapp.LedgerMessenger
import com.babylon.wallet.android.data.dapp.model.Curve
import com.babylon.wallet.android.data.dapp.model.DerivePublicKeyRequest
import com.babylon.wallet.android.data.dapp.model.LedgerDeviceModel.Companion.getLedgerDeviceModel
import com.babylon.wallet.android.domain.model.Selectable
import com.babylon.wallet.android.presentation.common.OneOffEvent
import com.babylon.wallet.android.presentation.common.OneOffEventHandler
import com.babylon.wallet.android.presentation.common.OneOffEventHandlerImpl
import com.babylon.wallet.android.presentation.common.StateViewModel
import com.babylon.wallet.android.presentation.common.UiMessage
import com.babylon.wallet.android.presentation.common.UiState
import com.babylon.wallet.android.presentation.model.AddLedgerSheetState
import com.babylon.wallet.android.presentation.model.LedgerDeviceUiModel
import com.babylon.wallet.android.utils.AppEvent
import com.babylon.wallet.android.utils.AppEventBus
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import rdx.works.core.UUIDGenerator
import rdx.works.profile.data.model.factorsources.LedgerHardwareWalletFactorSource
import rdx.works.profile.domain.AddLedgerFactorSourceUseCase
import rdx.works.profile.domain.GetProfileUseCase
import rdx.works.profile.domain.nextDerivationPathForAccountOnCurrentNetworkWithLedger
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
                    val selectedFactorSourceId = delegateState.selectedFactorSourceID
                    uiState.copy(
                        loading = delegateState.loading,
                        addLedgerSheetState = delegateState.addLedgerSheetState,
                        ledgerFactorSources = delegateState.ledgerFactorSources.map { ledgerFactorSource ->
                            Selectable(
                                data = ledgerFactorSource,
                                selected = ledgerFactorSource.id == selectedFactorSourceId
                            )
                        }.toPersistentList(),
                        waitingForLedgerResponse = delegateState.waitingForLedgerResponse,
                        recentlyConnectedLedgerDevice = delegateState.recentlyConnectedLedgerDevice,
                        hasP2pLinks = delegateState.hasP2pLinks,
                        uiMessage = delegateState.uiMessage
                    )
                }
            }
        }
    }

    override fun initialState(): CreateAccountWithLedgerState = CreateAccountWithLedgerState()

    fun onLedgerFactorSourceSelected(ledgerFactorSource: LedgerHardwareWalletFactorSource) {
        createLedgerDelegate.onLedgerFactorSourceSelected(ledgerFactorSource)
    }

    fun onSendAddLedgerRequest() {
        createLedgerDelegate.onSendAddLedgerRequest()
    }

    fun onSkipLedgerName() {
        createLedgerDelegate.onSkipLedgerName()
    }

    fun onUseLedger() {
        state.value.ledgerFactorSources.firstOrNull { it.selected }?.let { ledgerFactorSource ->
            viewModelScope.launch {
                _state.update { it.copy(waitingForLedgerResponse = true) }
                val derivationPath = getProfileUseCase.nextDerivationPathForAccountOnCurrentNetworkWithLedger(
                    ledgerHardwareWalletFactorSource = ledgerFactorSource.data
                )
                val deviceModel = requireNotNull(ledgerFactorSource.data.getLedgerDeviceModel())
                val result = ledgerMessenger.sendDerivePublicKeyRequest(
                    interactionId = UUIDGenerator.uuid().toString(),
                    keyParameters = listOf(DerivePublicKeyRequest.KeyParameters(Curve.Curve25519, derivationPath.path)),
                    ledgerDevice = DerivePublicKeyRequest.LedgerDevice(
                        name = ledgerFactorSource.data.hint.name,
                        model = deviceModel,
                        id = ledgerFactorSource.data.id.body.value
                    )
                )
                result.onSuccess { response ->
                    appEventBus.sendEvent(
                        AppEvent.DerivedAccountPublicKeyWithLedger(
                            factorSourceID = ledgerFactorSource.data.id,
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

    fun onMessageShown() {
        _state.update { it.copy(uiMessage = null) }
    }

    data class CreateAccountWithLedgerState(
        val loading: Boolean = false,
        val ledgerFactorSources: ImmutableList<Selectable<LedgerHardwareWalletFactorSource>> = persistentListOf(),
        val hasP2pLinks: Boolean = false,
        val addLedgerSheetState: AddLedgerSheetState = AddLedgerSheetState.Connect,
        val waitingForLedgerResponse: Boolean = false,
        val recentlyConnectedLedgerDevice: LedgerDeviceUiModel? = null,
        val uiMessage: UiMessage? = null
    ) : UiState
}

internal sealed interface CreateAccountWithLedgerEvent : OneOffEvent {
    object DerivedPublicKeyForAccount : CreateAccountWithLedgerEvent
}
