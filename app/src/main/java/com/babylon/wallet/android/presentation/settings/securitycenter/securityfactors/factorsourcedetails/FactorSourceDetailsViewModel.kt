package com.babylon.wallet.android.presentation.settings.securitycenter.securityfactors.factorsourcedetails

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.babylon.wallet.android.di.coroutines.DefaultDispatcher
import com.babylon.wallet.android.presentation.common.OneOffEvent
import com.babylon.wallet.android.presentation.common.OneOffEventHandler
import com.babylon.wallet.android.presentation.common.OneOffEventHandlerImpl
import com.babylon.wallet.android.presentation.common.StateViewModel
import com.babylon.wallet.android.presentation.common.UiMessage
import com.babylon.wallet.android.presentation.common.UiState
import com.babylon.wallet.android.presentation.ui.composables.RenameInput
import com.babylon.wallet.android.presentation.ui.composables.actionableaddress.ActionableAddress
import com.babylon.wallet.android.utils.callSafely
import com.radixdlt.sargon.FactorSource
import com.radixdlt.sargon.FactorSourceId
import com.radixdlt.sargon.FactorSourceKind
import com.radixdlt.sargon.extensions.asGeneral
import com.radixdlt.sargon.extensions.id
import com.radixdlt.sargon.extensions.kind
import com.radixdlt.sargon.extensions.name
import com.radixdlt.sargon.extensions.nonFungibleGlobalId
import com.radixdlt.sargon.os.SargonOsManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import rdx.works.core.preferences.PreferencesManager
import rdx.works.profile.data.repository.MnemonicRepository
import rdx.works.profile.domain.GetProfileUseCase
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class FactorSourceDetailsViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    getProfileUseCase: GetProfileUseCase,
    private val sargonOsManager: SargonOsManager,
    private val mnemonicRepository: MnemonicRepository,
    private val preferencesManager: PreferencesManager,
    @DefaultDispatcher private val defaultDispatcher: CoroutineDispatcher
) : StateViewModel<FactorSourceDetailsViewModel.State>(),
    OneOffEventHandler<FactorSourceDetailsViewModel.Event> by OneOffEventHandlerImpl() {

    private val args = FactorSourceDetailsArgs(savedStateHandle = savedStateHandle)

    override fun initialState(): State = State()

    init {
        combine(
            getProfileUseCase.flow.mapNotNull { profile ->
                profile.factorSources.firstOrNull { factorSource ->
                    factorSource.id == args.factorSourceId
                }
            },
            preferencesManager.getBackedUpFactorSourceIds()
        ) { factorSource, _ ->
            val deviceMnemonicLost = if (factorSource is FactorSource.Device) {
                !mnemonicRepository.mnemonicExist(factorSource.value.id.asGeneral())
            } else {
                false
            }

            _state.update { state ->
                state.copy(
                    factorSource = factorSource,
                    isDeviceFactorSourceMnemonicNotAvailable = deviceMnemonicLost
                )
            }
        }.flowOn(defaultDispatcher).launchIn(viewModelScope)
    }

    fun onRenameFactorSourceClick() {
        _state.update { state ->
            state.copy(
                isRenameBottomSheetVisible = true,
                renameFactorSourceInput = RenameFactorSourceInput(name = state.factorSourceName)
            )
        }
    }

    fun onRenameFactorSourceChanged(updatedName: String) {
        _state.update { state ->
            state.copy(
                renameFactorSourceInput = RenameFactorSourceInput(name = updatedName)
            )
        }
    }

    fun onRenameFactorSourceUpdateClick() {
        viewModelScope.launch {
            val currentFactorSource = state.value.factorSource ?: return@launch

            _state.update { state ->
                state.copy(
                    renameFactorSourceInput = state.renameFactorSourceInput.copy(isUpdating = true)
                )
            }

            sargonOsManager.callSafely(defaultDispatcher) {
                updateFactorSourceName(
                    factorSource = currentFactorSource,
                    name = state.value.renameFactorSourceInput.name
                )
            }.onFailure { error ->
                Timber.e("Failed to rename factor source: $error")
            }
            _state.update { state ->
                state.copy(
                    isRenameBottomSheetVisible = false,
                    uiMessage = UiMessage.InfoMessage.RenameSuccessful
                )
            }
        }
    }

    fun onRenameFactorSourceDismissed() {
        _state.update { state -> state.copy(isRenameBottomSheetVisible = false) }
    }

    fun onSpotCheckClick() {
        val currentFactorSource = state.value.factorSource ?: return
        viewModelScope.launch {
            sargonOsManager.callSafely(defaultDispatcher) {
                triggerSpotCheck(factorSource = currentFactorSource)
            }.onSuccess { isChecked ->
                _state.update { it.copy(uiMessage = UiMessage.InfoMessage.SpotCheckOutcome(isSuccess = isChecked)) }
            }.onFailure {
                _state.update { state ->
                    state.copy(
                        uiMessage = UiMessage.InfoMessage.SpotCheckOutcome(
                            isSuccess = false
                        )
                    )
                }
            }
        }
    }

    fun onViewSeedPhraseClick() {
        val deviceFactorSource = state.value.factorSource as? FactorSource.Device ?: return

        viewModelScope.launch {
            if (state.value.isDeviceFactorSourceMnemonicNotAvailable) {
                sendEvent(Event.NavigateToSeedPhraseRestore)
            } else {
                sendEvent(
                    Event.NavigateToSeedPhrase(
                        factorSourceId = deviceFactorSource.value.id.asGeneral()
                    )
                )
            }
        }
    }

    fun onNewMfaFactorInstanceClick() {
        val currentFactorSource = state.value.factorSource ?: return

        viewModelScope.launch {
            sargonOsManager.callSafely(defaultDispatcher) {
                val mfaFactorInstance = getNewMfaFactorInstance(currentFactorSource)
                mfaFactorInstance.nonFungibleGlobalId()
            }.onFailure {
                _state.update { state ->
                    state.copy(
                        uiMessage = UiMessage.ErrorMessage(it)
                    )
                }
            }.onSuccess {
                sendEvent(
                    Event.ShowAddress(
                        ActionableAddress.GlobalId(
                            address = it,
                            isVisitableInDashboard = true,
                            isOnlyLocalIdVisible = false
                        )
                    )
                )
            }
        }
    }

    fun onMessageShown() {
        _state.update { it.copy(uiMessage = null) }
    }

    data class State(
        val factorSource: FactorSource? = null,
        val renameFactorSourceInput: RenameFactorSourceInput = RenameFactorSourceInput(),
        val isFactorSourceNameUpdated: Boolean = false,
        val isRenameBottomSheetVisible: Boolean = false,
        val isDeviceFactorSourceMnemonicNotAvailable: Boolean = false,
        val uiMessage: UiMessage? = null
    ) : UiState {

        val factorSourceName: String = factorSource?.name.orEmpty()

        val factorSourceKind: FactorSourceKind =
            factorSource?.kind ?: FactorSourceKind.LEDGER_HQ_HARDWARE_WALLET
    }

    data class RenameFactorSourceInput(
        override val name: String = "",
        override val isUpdating: Boolean = false
    ) : RenameInput()

    sealed interface Event : OneOffEvent {

        data object NavigateBack : Event

        data class NavigateToSeedPhrase(val factorSourceId: FactorSourceId.Hash) : Event

        data object NavigateToSeedPhraseRestore : Event

        data class ShowAddress(
            val actionableAddress: ActionableAddress
        ) : Event
    }
}
