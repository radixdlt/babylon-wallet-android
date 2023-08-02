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
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import rdx.works.core.UUIDGenerator
import rdx.works.profile.data.model.factorsources.LedgerHardwareWalletFactorSource
import rdx.works.profile.domain.AddLedgerFactorSourceUseCase
import rdx.works.profile.domain.GetProfileUseCase
import rdx.works.profile.domain.nextDerivationPathForAccountOnCurrentNetworkWithLedger
import rdx.works.profile.domain.p2pLinks
import javax.inject.Inject

@HiltViewModel
class CreateAccountWithLedgerViewModel @Inject constructor(
    private val getProfileUseCase: GetProfileUseCase,
    private val ledgerMessenger: LedgerMessenger,
    addLedgerFactorSourceUseCase: AddLedgerFactorSourceUseCase,
    private val appEventBus: AppEventBus
) : StateViewModel<CreateAccountWithLedgerUiState>(),
    OneOffEventHandler<CreateAccountWithLedgerEvent> by OneOffEventHandlerImpl() {

    private val addLedgerDeviceDelegate = AddLedgerDeviceDelegate(
        getProfileUseCase = getProfileUseCase,
        ledgerMessenger = ledgerMessenger,
        addLedgerFactorSourceUseCase = addLedgerFactorSourceUseCase,
        scope = viewModelScope
    )

    init {
        viewModelScope.launch {
            addLedgerDeviceDelegate.state.collect { delegateState ->
                _state.update { uiState ->
                    val selectedLedgerDeviceId = delegateState.selectedLedgerDeviceId
                    uiState.copy(
                        loading = delegateState.loading,
                        addLedgerSheetState = delegateState.addLedgerSheetState,
                        ledgerDevices = delegateState.ledgerDevices.map { ledgerDevice ->
                            Selectable(
                                data = ledgerDevice,
                                selected = ledgerDevice.id == selectedLedgerDeviceId
                            )
                        }.toPersistentList(),
                        waitingForLedgerResponse = delegateState.waitingForLedgerResponse,
                        recentlyConnectedLedgerDevice = delegateState.recentlyConnectedLedgerDevice,
                        uiMessage = delegateState.uiMessage
                    )
                }
            }
        }
    }

    override fun initialState(): CreateAccountWithLedgerUiState = CreateAccountWithLedgerUiState()

    fun onLedgerDeviceSelected(ledgerFactorSource: LedgerHardwareWalletFactorSource) {
        addLedgerDeviceDelegate.onLedgerFactorSourceSelected(ledgerFactorSource)
    }

    fun onSendAddLedgerRequest() {
        addLedgerDeviceDelegate.onSendAddLedgerRequest()
    }

    fun onSkipLedgerName() {
        addLedgerDeviceDelegate.onSkipLedgerName()
    }

    fun onUseLedgerContinueClick() {
        state.value.ledgerDevices.firstOrNull { it.selected }?.let { ledgerFactorSource ->
            viewModelScope.launch {
                // check again if link connector exists
                if (getProfileUseCase.p2pLinks.first().isEmpty()) {
                    _state.update {
                        it.copy(showContent = CreateAccountWithLedgerUiState.ShowContent.LinkNewConnector)
                    }
                    return@launch
                }
                // if yes then proceed
                _state.update {
                    it.copy(waitingForLedgerResponse = true)
                }
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
        addLedgerDeviceDelegate.onConfirmLedgerName(name)
        _state.update {
            it.copy(showContent = CreateAccountWithLedgerUiState.ShowContent.ChooseLedger)
        }
    }

    fun onAddLedgerDeviceClick() {
        viewModelScope.launch {
            _state.update {
                if (getProfileUseCase.p2pLinks.first().isEmpty()) {
                    it.copy(showContent = CreateAccountWithLedgerUiState.ShowContent.LinkNewConnector)
                } else {
                    it.copy(showContent = CreateAccountWithLedgerUiState.ShowContent.AddLedger)
                }
            }
        }
    }

    fun onCloseClick() {
        _state.update {
            it.copy(showContent = CreateAccountWithLedgerUiState.ShowContent.ChooseLedger)
        }
    }

    fun onMessageShown() {
        _state.update {
            it.copy(uiMessage = null)
        }
    }

    fun onLinkConnectorClick() {
        _state.update {
            it.copy(showContent = CreateAccountWithLedgerUiState.ShowContent.AddLinkConnector)
        }
    }
}

data class CreateAccountWithLedgerUiState(
    val loading: Boolean = false,
    val ledgerDevices: ImmutableList<Selectable<LedgerHardwareWalletFactorSource>> = persistentListOf(),
    val showContent: ShowContent = ShowContent.ChooseLedger,
    val addLedgerSheetState: AddLedgerSheetState = AddLedgerSheetState.AddLedgerDevice,
    val waitingForLedgerResponse: Boolean = false,
    val recentlyConnectedLedgerDevice: LedgerDeviceUiModel? = null,
    val uiMessage: UiMessage? = null
) : UiState {

    enum class ShowContent {
        ChooseLedger, AddLedger, LinkNewConnector, AddLinkConnector
    }
}

internal sealed interface CreateAccountWithLedgerEvent : OneOffEvent {
    object DerivedPublicKeyForAccount : CreateAccountWithLedgerEvent
}
