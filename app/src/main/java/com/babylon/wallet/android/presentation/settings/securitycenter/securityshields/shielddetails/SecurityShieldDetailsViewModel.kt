package com.babylon.wallet.android.presentation.settings.securitycenter.securityshields.shielddetails

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.babylon.wallet.android.data.repository.securityshield.SecurityShieldBuilderClient
import com.babylon.wallet.android.di.coroutines.DefaultDispatcher
import com.babylon.wallet.android.domain.utils.AccessControllerTimedRecoveryStateObserver
import com.babylon.wallet.android.presentation.common.OneOffEvent
import com.babylon.wallet.android.presentation.common.OneOffEventHandler
import com.babylon.wallet.android.presentation.common.OneOffEventHandlerImpl
import com.babylon.wallet.android.presentation.common.StateViewModel
import com.babylon.wallet.android.presentation.common.UiMessage
import com.babylon.wallet.android.presentation.common.UiState
import com.babylon.wallet.android.presentation.ui.composables.RenameInput
import com.babylon.wallet.android.utils.callSafely
import com.radixdlt.sargon.DisplayName
import com.radixdlt.sargon.SecurityStructureOfFactorSources
import com.radixdlt.sargon.extensions.init
import com.radixdlt.sargon.os.SargonOsManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import rdx.works.profile.domain.GetProfileUseCase
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class SecurityShieldDetailsViewModel @Inject constructor(
    private val sargonOsManager: SargonOsManager,
    @DefaultDispatcher private val defaultDispatcher: CoroutineDispatcher,
    private val shieldBuilderClient: SecurityShieldBuilderClient,
    private val accessControllerTimedRecoveryStateObserver: AccessControllerTimedRecoveryStateObserver,
    getProfileUseCase: GetProfileUseCase,
    savedStateHandle: SavedStateHandle
) : StateViewModel<SecurityShieldDetailsViewModel.State>(),
    OneOffEventHandler<SecurityShieldDetailsViewModel.Event> by OneOffEventHandlerImpl() {

    private val args = SecurityShieldDetailsArgs(savedStateHandle)

    override fun initialState() = State()

    init {
        getProfileUseCase.flow.distinctUntilChanged()
            .onEach { getSecurityStructuresOfFactorSources(args.input) }
            .launchIn(viewModelScope)
        observeAccountRecoveryState()
    }

    private fun getSecurityStructuresOfFactorSources(input: SecurityShieldDetailsArgs.Input) {
        viewModelScope.launch {
            sargonOsManager.callSafely(defaultDispatcher) {
                when (input) {
                    is SecurityShieldDetailsArgs.Input.Address -> securityStructureOfFactorSourcesFromAddressOfAccountOrPersona(
                        addressOfAccountOrPersona = input.value
                    )

                    is SecurityShieldDetailsArgs.Input.Id -> securityStructuresOfFactorSources()
                        .first { it.metadata.id == input.value }
                }
            }.onSuccess { securityStructureOfFactorSources ->
                _state.update { state ->
                    state.copy(
                        securityShieldName = securityStructureOfFactorSources.metadata.displayName.value,
                        securityStructureOfFactorSources = securityStructureOfFactorSources,
                        isShieldApplied = input is SecurityShieldDetailsArgs.Input.Address,
                    )
                }
            }.onFailure {
                Timber.e("Failed to get security structure.")
            }
        }
    }

    fun onRenameSecurityShieldClick() {
        _state.update { state ->
            state.copy(
                isRenameBottomSheetVisible = true,
                renameSecurityShieldInput = State.RenameSecurityShieldInput(name = state.securityShieldName)
            )
        }
    }

    fun onRenameSecurityShieldChanged(updatedName: String) {
        _state.update { state ->
            state.copy(
                renameSecurityShieldInput = State.RenameSecurityShieldInput(name = updatedName)
            )
        }
    }

    fun onRenameSecurityShieldUpdateClick() {
        viewModelScope.launch {
            _state.update { state ->
                state.copy(
                    renameSecurityShieldInput = state.renameSecurityShieldInput.copy(isUpdating = true)
                )
            }

            sargonOsManager.callSafely(defaultDispatcher) {
                renameSecurityStructure(
                    securityStructureId = (args.input as SecurityShieldDetailsArgs.Input.Id).value,
                    name = DisplayName.init(state.value.renameSecurityShieldInput.name)
                )
            }.onFailure { error ->
                Timber.e("Failed to rename security shield: $error")

                _state.update { state ->
                    state.copy(
                        isRenameBottomSheetVisible = false,
                        uiMessage = UiMessage.ErrorMessage(error)
                    )
                }
            }.onSuccess {
                _state.update { state ->
                    state.copy(
                        isRenameBottomSheetVisible = false,
                        uiMessage = UiMessage.InfoMessage.RenameSuccessful
                    )
                }
            }
        }
    }

    fun onRenameSecurityShieldDismissed() {
        _state.update { state -> state.copy(isRenameBottomSheetVisible = false) }
    }

    fun onEditFactorsClick() {
        val shield = state.value.securityStructureOfFactorSources ?: return

        viewModelScope.launch {
            if (args.input is SecurityShieldDetailsArgs.Input.Address) {
                shieldBuilderClient.withAppliedSecurityStructure(shield, args.input.value)
            } else {
                shieldBuilderClient.withExistingSecurityStructure(shield)
            }
            sendEvent(Event.EditShield)
        }
    }

    private fun observeAccountRecoveryState() {
        val address = (args.input as? SecurityShieldDetailsArgs.Input.Address)?.value ?: return

        accessControllerTimedRecoveryStateObserver.recoveryStateByAddress
            .onEach { states ->
                val recoveryState = states[address]
                _state.update { state ->
                    state.copy(
                        canEditShield = recoveryState?.timedRecoveryState == null
                    )
                }
            }
            .launchIn(viewModelScope)
    }

    data class State(
        val securityShieldName: String = "",
        val securityStructureOfFactorSources: SecurityStructureOfFactorSources? = null,
        val isRenameBottomSheetVisible: Boolean = false,
        val renameSecurityShieldInput: RenameSecurityShieldInput = RenameSecurityShieldInput(),
        val uiMessage: UiMessage? = null,
        val isShieldApplied: Boolean = false,
        val canEditShield: Boolean = false
    ) : UiState {

        data class RenameSecurityShieldInput(
            override val name: String = "",
            override val isUpdating: Boolean = false
        ) : RenameInput()
    }

    sealed interface Event : OneOffEvent {

        data object EditShield : Event
    }
}
