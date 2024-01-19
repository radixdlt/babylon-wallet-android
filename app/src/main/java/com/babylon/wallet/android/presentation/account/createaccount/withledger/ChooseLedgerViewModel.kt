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
import com.babylon.wallet.android.presentation.settings.accountsecurity.ledgerhardwarewallets.ShowLinkConnectorPromptState
import com.babylon.wallet.android.utils.AppEvent
import com.babylon.wallet.android.utils.AppEventBus
import com.babylon.wallet.android.utils.Constants
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import rdx.works.core.UUIDGenerator
import rdx.works.profile.data.model.factorsources.DerivationPathScheme
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
class ChooseLedgerViewModel @Inject constructor(
    private val getProfileUseCase: GetProfileUseCase,
    private val getCurrentGatewayUseCase: GetCurrentGatewayUseCase,
    private val ledgerMessenger: LedgerMessenger,
    private val ensureBabylonFactorSourceExistUseCase: EnsureBabylonFactorSourceExistUseCase,
    private val appEventBus: AppEventBus,
    savedStateHandle: SavedStateHandle
) : StateViewModel<ChooseLedgerUiState>(),
    OneOffEventHandler<ChooseLedgerEvent> by OneOffEventHandlerImpl() {

    private val args = ChooserLedgerArgs(savedStateHandle)

    override fun initialState(): ChooseLedgerUiState = ChooseLedgerUiState()

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

    fun dismissConnectorPrompt(linkConnector: Boolean, source: ShowLinkConnectorPromptState.Source) {
        _state.update {
            it.copy(
                showContent = if (linkConnector) {
                    ChooseLedgerUiState.ShowContent.LinkNewConnector(
                        source == ShowLinkConnectorPromptState.Source.AddLedgerDevice
                    )
                } else {
                    it.showContent
                },
                showLinkConnectorPromptState = ShowLinkConnectorPromptState.None
            )
        }
    }

    @Suppress("LongMethod")
    fun onUseLedgerContinueClick(deviceBiometricAuthenticationProvider: suspend () -> Boolean) {
        state.value.ledgerDevices.firstOrNull { selectableLedgerDevice ->
            selectableLedgerDevice.selected
        }?.let { ledgerFactorSource ->
            viewModelScope.launch {
                val hasAtLeastOneLinkedConnector = getProfileUseCase.p2pLinks.first().isNotEmpty()
                // check if there is not linked connector and show link new connector screen
                if (hasAtLeastOneLinkedConnector.not()) {
                    _state.update {
                        it.copy(showContent = ChooseLedgerUiState.ShowContent.LinkNewConnector(false))
                    }
                    return@launch
                }

                when (args.ledgerSelectionPurpose) {
                    LedgerSelectionPurpose.CreateAccount -> {
                        // check again if link connector exists
                        if (ensureBabylonFactorSourceExistUseCase.babylonFactorSourceExist().not()) {
                            val authenticationResult = deviceBiometricAuthenticationProvider()
                            if (authenticationResult) {
                                ensureBabylonFactorSourceExistUseCase()
                            } else {
                                // don't move forward without babylon factor source
                                return@launch
                            }
                        }
                        val derivationPath = getProfileUseCase.nextDerivationPathForAccountOnNetwork(
                            DerivationPathScheme.CAP_26,
                            networkIdToCreateAccountOn(),
                            ledgerFactorSource.data.id
                        )
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
                            sendEvent(ChooseLedgerEvent.DerivedPublicKeyForAccount)
                        }
                    }

                    LedgerSelectionPurpose.RecoveryScanBabylon,
                    LedgerSelectionPurpose.RecoveryScanOlympia -> {
                        sendEvent(
                            ChooseLedgerEvent.RecoverAccounts(
                                ledgerFactorSource.data,
                                args.ledgerSelectionPurpose == LedgerSelectionPurpose.RecoveryScanOlympia
                            )
                        )
                    }
                }
            }
        }
    }

    private suspend fun networkIdToCreateAccountOn(): Int {
        return if (args.networkId == Constants.USE_CURRENT_NETWORK) {
            getCurrentGatewayUseCase.invoke().network.id
        } else {
            args.networkId
        }
    }

    fun onAddLedgerDeviceClick() {
        viewModelScope.launch {
            _state.update { uiState ->
                val hasAtLeastOneLinkedConnector = getProfileUseCase.p2pLinks.first().isNotEmpty()
                if (hasAtLeastOneLinkedConnector) {
                    uiState.copy(showContent = ChooseLedgerUiState.ShowContent.AddLedger)
                } else {
                    uiState.copy(showContent = ChooseLedgerUiState.ShowContent.LinkNewConnector())
                }
            }
        }
    }

    private fun showAddLedgerDeviceContent() {
        _state.update {
            it.copy(showContent = ChooseLedgerUiState.ShowContent.AddLedger)
        }
    }

    fun onCloseClick() {
        _state.update {
            it.copy(showContent = ChooseLedgerUiState.ShowContent.ChooseLedger)
        }
    }

    fun onNewLinkConnectorAdded(addDeviceAfterLinking: Boolean) {
        _state.update { it.copy(isAddingLinkConnector = true) }
        if (addDeviceAfterLinking) {
            showAddLedgerDeviceContent()
        } else {
            onCloseClick()
        }
        viewModelScope.launch {
            ledgerMessenger.isAnyLinkedConnectorConnected.filter { it }.firstOrNull()?.let {
                _state.update { state -> state.copy(isAddingLinkConnector = false) }
            }
        }
    }

    fun onLinkConnectorClick(addDeviceAfterLinking: Boolean) {
        _state.update {
            it.copy(showContent = ChooseLedgerUiState.ShowContent.AddLinkConnector(addDeviceAfterLinking))
        }
    }
}

data class ChooseLedgerUiState(
    val loading: Boolean = false,
    val showContent: ShowContent = ShowContent.ChooseLedger,
    val ledgerDevices: ImmutableList<Selectable<LedgerHardwareWalletFactorSource>> = persistentListOf(),
    val selectedLedgerDeviceId: FactorSource.FactorSourceID.FromHash? = null,
    val isLinkConnectionEstablished: Boolean = false,
    val showLinkConnectorPromptState: ShowLinkConnectorPromptState = ShowLinkConnectorPromptState.None,
    val isAddingLinkConnector: Boolean = false
) : UiState {

    sealed interface ShowContent {
        data object ChooseLedger : ShowContent
        data object AddLedger : ShowContent
        data class LinkNewConnector(val addDeviceAfterLinking: Boolean = true) : ShowContent
        data class AddLinkConnector(val addDeviceAfterLinking: Boolean = true) : ShowContent
    }
}

internal sealed interface ChooseLedgerEvent : OneOffEvent {
    data object DerivedPublicKeyForAccount : ChooseLedgerEvent
    data class RecoverAccounts(val factorSource: FactorSource, val isOlympia: Boolean) : ChooseLedgerEvent
}
