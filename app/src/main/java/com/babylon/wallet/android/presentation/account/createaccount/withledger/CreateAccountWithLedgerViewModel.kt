package com.babylon.wallet.android.presentation.account.createaccount.withledger

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.babylon.wallet.android.data.dapp.LedgerMessenger
import com.babylon.wallet.android.data.dapp.model.Curve
import com.babylon.wallet.android.data.dapp.model.LedgerInteractionRequest
import com.babylon.wallet.android.domain.model.Selectable
import com.babylon.wallet.android.presentation.common.OneOffEvent
import com.babylon.wallet.android.presentation.common.OneOffEventHandler
import com.babylon.wallet.android.presentation.common.OneOffEventHandlerImpl
import com.babylon.wallet.android.presentation.common.StateViewModel
import com.babylon.wallet.android.presentation.common.UiState
import com.babylon.wallet.android.utils.AppEvent
import com.babylon.wallet.android.utils.AppEventBus
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import rdx.works.core.UUIDGenerator
import rdx.works.profile.data.model.factorsources.FactorSource
import rdx.works.profile.data.model.factorsources.LedgerHardwareWalletFactorSource
import rdx.works.profile.domain.EnsureBabylonFactorSourceExistUseCase
import rdx.works.profile.domain.GetProfileUseCase
import rdx.works.profile.domain.gateway.GetCurrentGatewayUseCase
import rdx.works.profile.domain.ledgerFactorSources
import rdx.works.profile.domain.nextDerivationPathForAccountOnNetwork
import rdx.works.profile.domain.p2pLinks
import javax.inject.Inject

@HiltViewModel
class CreateAccountWithLedgerViewModel @Inject constructor(
    private val getProfileUseCase: GetProfileUseCase,
    private val getCurrentGatewayUseCase: GetCurrentGatewayUseCase,
    private val ledgerMessenger: LedgerMessenger,
    private val ensureBabylonFactorSourceExistUseCase: EnsureBabylonFactorSourceExistUseCase,
    private val appEventBus: AppEventBus,
    savedStateHandle: SavedStateHandle
) : StateViewModel<CreateAccountWithLedgerUiState>(),
    OneOffEventHandler<CreateAccountWithLedgerEvent> by OneOffEventHandlerImpl() {

    private val args = CreateAccountWithLedgerArgs(savedStateHandle)

    override fun initialState(): CreateAccountWithLedgerUiState = CreateAccountWithLedgerUiState()

    init {
        viewModelScope.launch {
            getProfileUseCase.ledgerFactorSources.collect { ledgerDevices ->
                _state.update { uiState ->
                    uiState.copy(
                        loading = false,
                        ledgerDevices = ledgerDevices.mapIndexed { index, ledgerDevice ->
                            if (uiState.selectedLedgerDeviceId == null && index == 0) {
                                Selectable(
                                    data = ledgerDevice,
                                    selected = true
                                )
                            } else {
                                Selectable(
                                    data = ledgerDevice,
                                    selected = ledgerDevice.id == uiState.selectedLedgerDeviceId
                                )
                            }
                        }.toPersistentList()
                    )
                }
            }
        }
    }

    fun onLedgerDeviceSelected(selectedLedgerDevice: LedgerHardwareWalletFactorSource) {
        _state.update { uiState ->
            uiState.copy(
                selectedLedgerDeviceId = selectedLedgerDevice.id,
                ledgerDevices = uiState.ledgerDevices.map { selectableLedgerDevice ->
                    val updatedSelectableLedgerDevice = selectableLedgerDevice.copy(
                        selected = selectableLedgerDevice.data.id == selectedLedgerDevice.id
                    )
                    updatedSelectableLedgerDevice
                }.toImmutableList()
            )
        }
    }

    fun onUseLedgerContinueClick(deviceBiometricAuthenticationProvider: suspend () -> Boolean) {
        state.value.ledgerDevices.firstOrNull { selectableLedgerDevice ->
            selectableLedgerDevice.selected
        }?.let { ledgerFactorSource ->
            viewModelScope.launch {
                // check again if link connector exists
                if (getProfileUseCase.p2pLinks.first().isEmpty()) {
                    _state.update {
                        it.copy(showContent = CreateAccountWithLedgerUiState.ShowContent.LinkNewConnector(false))
                    }
                    return@launch
                }
                if (ensureBabylonFactorSourceExistUseCase.babylonFactorSourceExist().not()) {
                    val authenticationResult = deviceBiometricAuthenticationProvider()
                    if (authenticationResult) {
                        ensureBabylonFactorSourceExistUseCase()
                    } else {
                        // don't move forward without babylon factor source
                        return@launch
                    }
                }
                val derivationPath = getProfileUseCase.nextDerivationPathForAccountOnNetwork(networkIdToCreateAccountOn())
                val result = ledgerMessenger.sendDerivePublicKeyRequest(
                    interactionId = UUIDGenerator.uuid().toString(),
                    keyParameters = listOf(LedgerInteractionRequest.KeyParameters(Curve.Curve25519, derivationPath.path)),
                    ledgerDevice = LedgerInteractionRequest.LedgerDevice.from(ledgerFactorSource.data)
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

    private suspend fun networkIdToCreateAccountOn(): Int {
        return if (args.networkId == -1) {
            getCurrentGatewayUseCase.invoke().network.id
        } else {
            args.networkId
        }
    }

    fun onAddLedgerDeviceClick() {
        viewModelScope.launch {
            _state.update {
                if (getProfileUseCase.p2pLinks.first().isEmpty()) {
                    it.copy(showContent = CreateAccountWithLedgerUiState.ShowContent.LinkNewConnector())
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

    fun onLinkConnectorClick(addDeviceAfterLinking: Boolean) {
        _state.update {
            it.copy(showContent = CreateAccountWithLedgerUiState.ShowContent.AddLinkConnector(addDeviceAfterLinking))
        }
    }
}

data class CreateAccountWithLedgerUiState(
    val loading: Boolean = false,
    val showContent: ShowContent = ShowContent.ChooseLedger,
    val ledgerDevices: ImmutableList<Selectable<LedgerHardwareWalletFactorSource>> = persistentListOf(),
    val selectedLedgerDeviceId: FactorSource.FactorSourceID.FromHash? = null
) : UiState {

    sealed interface ShowContent {
        data object ChooseLedger : ShowContent
        data object AddLedger : ShowContent
        data class LinkNewConnector(val addDeviceAfterLinking: Boolean = true) : ShowContent
        data class AddLinkConnector(val addDeviceAfterLinking: Boolean = true) : ShowContent
    }
}

internal sealed interface CreateAccountWithLedgerEvent : OneOffEvent {
    object DerivedPublicKeyForAccount : CreateAccountWithLedgerEvent
}
