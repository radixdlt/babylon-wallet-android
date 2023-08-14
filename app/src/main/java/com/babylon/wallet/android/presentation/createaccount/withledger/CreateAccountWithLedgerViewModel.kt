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
import rdx.works.profile.domain.GetProfileUseCase
import rdx.works.profile.domain.ledgerFactorSources
import rdx.works.profile.domain.nextDerivationPathForAccountOnCurrentNetworkWithLedger
import rdx.works.profile.domain.p2pLinks
import javax.inject.Inject

@HiltViewModel
class CreateAccountWithLedgerViewModel @Inject constructor(
    private val getProfileUseCase: GetProfileUseCase,
    private val ledgerMessenger: LedgerMessenger,
    private val appEventBus: AppEventBus
) : StateViewModel<CreateAccountWithLedgerUiState>(),
    OneOffEventHandler<CreateAccountWithLedgerEvent> by OneOffEventHandlerImpl() {

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

    fun onUseLedgerContinueClick() {
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
        object ChooseLedger : ShowContent
        object AddLedger : ShowContent
        data class LinkNewConnector(val addDeviceAfterLinking: Boolean = true) : ShowContent
        data class AddLinkConnector(val addDeviceAfterLinking: Boolean = true) : ShowContent
    }
}

internal sealed interface CreateAccountWithLedgerEvent : OneOffEvent {
    object DerivedPublicKeyForAccount : CreateAccountWithLedgerEvent
}
