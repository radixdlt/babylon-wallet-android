package com.babylon.wallet.android.presentation.settings.securitycenter.securityshields.shielddetails

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.babylon.wallet.android.data.repository.securityshield.SecurityShieldBuilderClient
import com.babylon.wallet.android.di.coroutines.DefaultDispatcher
import com.babylon.wallet.android.presentation.common.OneOffEvent
import com.babylon.wallet.android.presentation.common.OneOffEventHandler
import com.babylon.wallet.android.presentation.common.OneOffEventHandlerImpl
import com.babylon.wallet.android.presentation.common.StateViewModel
import com.babylon.wallet.android.presentation.common.UiMessage
import com.babylon.wallet.android.presentation.common.UiState
import com.babylon.wallet.android.presentation.ui.composables.RenameInput
import com.babylon.wallet.android.utils.callSafely
import com.radixdlt.sargon.Account
import com.radixdlt.sargon.DisplayName
import com.radixdlt.sargon.Persona
import com.radixdlt.sargon.ProfileToCheck
import com.radixdlt.sargon.SecurityStructureId
import com.radixdlt.sargon.SecurityStructureOfFactorSources
import com.radixdlt.sargon.extensions.init
import com.radixdlt.sargon.os.SargonOsManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import rdx.works.core.sargon.toIds
import rdx.works.profile.domain.GetProfileUseCase
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class SecurityShieldDetailsViewModel @Inject constructor(
    private val sargonOsManager: SargonOsManager,
    @DefaultDispatcher private val defaultDispatcher: CoroutineDispatcher,
    private val shieldBuilderClient: SecurityShieldBuilderClient,
    getProfileUseCase: GetProfileUseCase,
    savedStateHandle: SavedStateHandle
) : StateViewModel<SecurityShieldDetailsViewModel.State>(),
    OneOffEventHandler<SecurityShieldDetailsViewModel.Event> by OneOffEventHandlerImpl() {

    private val args = SecurityShieldDetailsArgs(savedStateHandle)

    override fun initialState() = State()

    init {
        getProfileUseCase.flow.map { it.appPreferences.security.securityStructuresOfFactorSourceIds }
            .distinctUntilChanged()
            .onEach {
                getSecurityStructuresOfFactorSources(shieldId = args.securityStructureId)
                getEntitiesLinkedToSecurityStructure(shieldId = args.securityStructureId)
            }
            .launchIn(viewModelScope)
    }

    private fun getSecurityStructuresOfFactorSources(shieldId: SecurityStructureId) {
        viewModelScope.launch {
            sargonOsManager.callSafely(defaultDispatcher) {
                securityStructuresOfFactorSources()
                    .first { it.metadata.id == shieldId }
            }.onSuccess { securityStructureOfFactorSources ->
                _state.update { state ->
                    state.copy(
                        securityShieldName = securityStructureOfFactorSources.metadata.displayName.value,
                        securityStructureOfFactorSources = securityStructureOfFactorSources
                    )
                }
            }.onFailure {
                Timber.e("Failed to get security structure.")
            }
        }
    }

    private fun getEntitiesLinkedToSecurityStructure(shieldId: SecurityStructureId) {
        viewModelScope.launch {
            sargonOsManager.callSafely(defaultDispatcher) {
                entitiesLinkedToSecurityStructure(
                    shieldId = shieldId,
                    profileToCheck = ProfileToCheck.Current
                )
            }.onSuccess { linkedEntities ->
                _state.update { state ->
                    state.copy(
                        linkedAccounts = linkedEntities.accounts.toPersistentList(),
                        linkedPersonas = linkedEntities.personas.toPersistentList(),
                        hasAnyHiddenLinkedEntities = linkedEntities.hiddenAccounts.isNotEmpty() ||
                            linkedEntities.hiddenPersonas.isNotEmpty()
                    )
                }
            }.onFailure {
                Timber.e("Failed to get linked entities for security structure id: $shieldId")
            }
        }
    }

    fun onRenameSecurityShieldClick() {
        _state.update { state ->
            state.copy(
                isRenameBottomSheetVisible = true,
                renameSecurityShieldInput = RenameSecurityShieldInput(name = state.securityShieldName)
            )
        }
    }

    fun onRenameSecurityShieldChanged(updatedName: String) {
        _state.update { state ->
            state.copy(
                renameSecurityShieldInput = RenameSecurityShieldInput(name = updatedName)
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
                    securityStructureId = args.securityStructureId,
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
            shieldBuilderClient.withExistingSecurityStructure(shield.toIds())
            sendEvent(Event.EditShield)
        }
    }

    data class State(
        val isLoading: Boolean = true,
        val securityShieldName: String = "",
        val securityStructureOfFactorSources: SecurityStructureOfFactorSources? = null,
        val linkedAccounts: PersistentList<Account> = persistentListOf(),
        val linkedPersonas: PersistentList<Persona> = persistentListOf(),
        val hasAnyHiddenLinkedEntities: Boolean = false,
        val isRenameBottomSheetVisible: Boolean = false,
        val renameSecurityShieldInput: RenameSecurityShieldInput = RenameSecurityShieldInput(),
        val uiMessage: UiMessage? = null
    ) : UiState

    data class RenameSecurityShieldInput(
        override val name: String = "",
        override val isUpdating: Boolean = false
    ) : RenameInput()

    sealed interface Event : OneOffEvent {

        data object EditShield : Event
    }
}
