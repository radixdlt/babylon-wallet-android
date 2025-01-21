package com.babylon.wallet.android.presentation.settings.securitycenter.securityfactors.factorsourcedetails

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.babylon.wallet.android.di.coroutines.DefaultDispatcher
import com.babylon.wallet.android.domain.usecases.BiometricsAuthenticateUseCase
import com.babylon.wallet.android.presentation.common.OneOffEvent
import com.babylon.wallet.android.presentation.common.OneOffEventHandler
import com.babylon.wallet.android.presentation.common.OneOffEventHandlerImpl
import com.babylon.wallet.android.presentation.common.StateViewModel
import com.babylon.wallet.android.presentation.common.UiState
import com.babylon.wallet.android.presentation.ui.composables.RenameInput
import com.babylon.wallet.android.utils.callSafely
import com.radixdlt.sargon.FactorSource
import com.radixdlt.sargon.FactorSourceId
import com.radixdlt.sargon.FactorSourceKind
import com.radixdlt.sargon.extensions.asGeneral
import com.radixdlt.sargon.extensions.id
import com.radixdlt.sargon.extensions.kind
import com.radixdlt.sargon.extensions.name
import com.radixdlt.sargon.os.SargonOsManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import rdx.works.profile.domain.GetProfileUseCase
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class FactorSourceDetailsViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    getProfileUseCase: GetProfileUseCase,
    private val sargonOsManager: SargonOsManager,
    private val biometricsAuthenticateUseCase: BiometricsAuthenticateUseCase,
    @DefaultDispatcher private val defaultDispatcher: CoroutineDispatcher
) : StateViewModel<FactorSourceDetailsViewModel.State>(),
    OneOffEventHandler<FactorSourceDetailsViewModel.Event> by OneOffEventHandlerImpl() {

    private var currentFactorSource: FactorSource? = null

    override fun initialState(): State = State()

    init {
        viewModelScope.launch {
            val factorSourceId = FactorSourceDetailsArgs(savedStateHandle = savedStateHandle).factorSourceId

            getProfileUseCase.flow
                .mapNotNull { profile ->
                    profile.factorSources.firstOrNull { factorSource ->
                        factorSource.id == factorSourceId
                    }
                }
                .collectLatest { factorSource ->
                    currentFactorSource = factorSource
                    _state.update { state ->
                        state.copy(
                            factorSourceName = factorSource.name,
                            factorSourceKind = factorSource.kind
                        )
                    }
                }
        }
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
            _state.update { state ->
                state.copy(
                    renameFactorSourceInput = state.renameFactorSourceInput.copy(isUpdating = true)
                )
            }
            currentFactorSource?.let {
                sargonOsManager.callSafely(defaultDispatcher) {
                    updateFactorSourceName(
                        factorSource = it,
                        name = state.value.renameFactorSourceInput.name
                    )
                }.onFailure { error ->
                    Timber.e("Failed to rename factor source: $error")
                }
            }
            _state.update { state ->
                state.copy(
                    isRenameBottomSheetVisible = false,
                    isFactorSourceNameUpdated = true
                )
            }
        }
    }

    fun onRenameFactorSourceDismissed() {
        _state.update { state -> state.copy(isRenameBottomSheetVisible = false) }
    }

    fun onSnackbarMessageShown() {
        _state.update { state -> state.copy(isFactorSourceNameUpdated = false) }
    }

    fun onSpotCheckClick() {
        // TODO
    }

    fun onViewSeedPhraseClick() {
        viewModelScope.launch {
            // TODO if factor source is lost then navigate to restore mnemonic
            val deviceFactorSource = currentFactorSource as? FactorSource.Device
            deviceFactorSource?.let {
                if (biometricsAuthenticateUseCase()) {
                    sendEvent(Event.NavigateToSeedPhrase(factorSourceId = deviceFactorSource.value.id.asGeneral()))
                } else {
                    return@launch
                }
            }
        }
    }

    @Suppress("UnusedParameter")
    fun onArculusPinCheckedChange(isChecked: Boolean) {
        // TODO
    }

    fun onChangeArculusPinClick() {
        // TODO
    }

    data class State(
        val factorSourceName: String = "",
        val factorSourceKind: FactorSourceKind = FactorSourceKind.LEDGER_HQ_HARDWARE_WALLET,
        val renameFactorSourceInput: RenameFactorSourceInput = RenameFactorSourceInput(),
        val isFactorSourceNameUpdated: Boolean = false,
        val isRenameBottomSheetVisible: Boolean = false,
        val isArculusPinEnabled: Boolean = false
    ) : UiState

    data class RenameFactorSourceInput(
        override val name: String = "",
        override val isUpdating: Boolean = false
    ) : RenameInput()

    sealed interface Event : OneOffEvent {

        data object NavigateBack : Event

        data class NavigateToSeedPhrase(val factorSourceId: FactorSourceId.Hash) : Event
    }
}
