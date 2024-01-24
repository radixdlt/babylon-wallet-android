package com.babylon.wallet.android.presentation.account.createaccount.withledger

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.babylon.wallet.android.data.dapp.LedgerMessenger
import com.babylon.wallet.android.domain.model.Selectable
import com.babylon.wallet.android.presentation.common.OneOffEvent
import com.babylon.wallet.android.presentation.common.OneOffEventHandler
import com.babylon.wallet.android.presentation.common.OneOffEventHandlerImpl
import com.babylon.wallet.android.presentation.common.StateViewModel
import com.babylon.wallet.android.presentation.common.UiMessage
import com.babylon.wallet.android.presentation.common.UiState
import com.babylon.wallet.android.presentation.settings.accountsecurity.ledgerhardwarewallets.ShowLinkConnectorPromptState
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
import rdx.works.profile.data.model.factorsources.FactorSource
import rdx.works.profile.data.model.factorsources.LedgerHardwareWalletFactorSource
import rdx.works.profile.domain.EnsureBabylonFactorSourceExistUseCase
import rdx.works.profile.domain.GetProfileUseCase
import rdx.works.profile.domain.ledgerFactorSources
import rdx.works.profile.domain.p2pLinks
import javax.inject.Inject

@HiltViewModel
class ChooseLedgerViewModel @Inject constructor(
    private val getProfileUseCase: GetProfileUseCase,
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

    fun dismissConnectorPrompt(linkConnector: Boolean) {
        _state.update {
            it.copy(
                showContent = if (linkConnector) {
                    ChooseLedgerUiState.ShowContent.LinkNewConnector
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
                        it.copy(
                            shouldShowChooseLedgerContentAfterNewLinkedConnector = true,
                            showContent = ChooseLedgerUiState.ShowContent.LinkNewConnector
                        )
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
                        appEventBus.sendEvent(
                            AppEvent.AccessFactorSources.SelectedLedgerDevice(ledgerFactorSource = ledgerFactorSource.data)
                        )
                        sendEvent(ChooseLedgerEvent.LedgerSelected)
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

    fun onAddLedgerDeviceClick() {
        viewModelScope.launch {
            _state.update { uiState ->
                val hasAtLeastOneLinkedConnector = getProfileUseCase.p2pLinks.first().isNotEmpty()
                if (hasAtLeastOneLinkedConnector) {
                    uiState.copy(showContent = ChooseLedgerUiState.ShowContent.AddLedger)
                } else {
                    uiState.copy(
                        shouldShowChooseLedgerContentAfterNewLinkedConnector = false,
                        showContent = ChooseLedgerUiState.ShowContent.LinkNewConnector
                    )
                }
            }
        }
    }

    fun onLinkConnectorClick() {
        _state.update {
            it.copy(showContent = ChooseLedgerUiState.ShowContent.AddLinkConnector)
        }
    }

    fun onNewLinkedConnectorAdded() {
        if (_state.value.shouldShowChooseLedgerContentAfterNewLinkedConnector) {
            _state.update {
                it.copy(showContent = ChooseLedgerUiState.ShowContent.ChooseLedger)
            }
        } else {
            _state.update {
                it.copy(showContent = ChooseLedgerUiState.ShowContent.AddLedger)
            }
        }
    }

    fun onCloseClick() {
        _state.update {
            it.copy(showContent = ChooseLedgerUiState.ShowContent.ChooseLedger)
        }
    }

    fun onMessageShown() {
        _state.update { it.copy(uiMessage = null) }
    }
}

data class ChooseLedgerUiState(
    val loading: Boolean = false,
    val showContent: ShowContent = ShowContent.ChooseLedger,
    val ledgerDevices: ImmutableList<Selectable<LedgerHardwareWalletFactorSource>> = persistentListOf(),
    val selectedLedgerDeviceId: FactorSource.FactorSourceID.FromHash? = null,
    val showLinkConnectorPromptState: ShowLinkConnectorPromptState = ShowLinkConnectorPromptState.None,
    val shouldShowChooseLedgerContentAfterNewLinkedConnector: Boolean = false,
    val uiMessage: UiMessage? = null
) : UiState {

    sealed interface ShowContent {
        data object ChooseLedger : ShowContent
        data object AddLedger : ShowContent
        data object LinkNewConnector : ShowContent
        data object AddLinkConnector : ShowContent
    }
}

internal sealed interface ChooseLedgerEvent : OneOffEvent {
    data object LedgerSelected : ChooseLedgerEvent
    data class RecoverAccounts(val factorSource: FactorSource, val isOlympia: Boolean) : ChooseLedgerEvent
}
