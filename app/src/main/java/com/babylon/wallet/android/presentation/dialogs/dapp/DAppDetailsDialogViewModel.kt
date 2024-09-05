package com.babylon.wallet.android.presentation.dialogs.dapp

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.babylon.wallet.android.domain.model.DAppWithResources
import com.babylon.wallet.android.domain.usecases.ChangeLockerDepositsVisibilityUseCase
import com.babylon.wallet.android.domain.usecases.GetDAppWithResourcesUseCase
import com.babylon.wallet.android.domain.usecases.GetValidatedDAppWebsiteUseCase
import com.babylon.wallet.android.presentation.common.OneOffEvent
import com.babylon.wallet.android.presentation.common.OneOffEventHandler
import com.babylon.wallet.android.presentation.common.OneOffEventHandlerImpl
import com.babylon.wallet.android.presentation.common.StateViewModel
import com.babylon.wallet.android.presentation.common.UiMessage
import com.babylon.wallet.android.presentation.common.UiState
import com.radixdlt.sargon.AccountAddress
import com.radixdlt.sargon.AuthorizedDapp
import com.radixdlt.sargon.Persona
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import rdx.works.core.sargon.activePersonaOnCurrentNetwork
import rdx.works.profile.data.repository.DAppConnectionRepository
import rdx.works.profile.domain.GetProfileUseCase
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class DAppDetailsDialogViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val dAppConnectionRepository: DAppConnectionRepository,
    private val getProfileUseCase: GetProfileUseCase,
    private val changeLockerDepositsVisibilityUseCase: ChangeLockerDepositsVisibilityUseCase,
    getDAppWithResourcesUseCase: GetDAppWithResourcesUseCase,
    getValidatedDAppWebsiteUseCase: GetValidatedDAppWebsiteUseCase
) : StateViewModel<DAppDetailsDialogViewModel.State>(), OneOffEventHandler<DAppDetailsDialogViewModel.Event> by OneOffEventHandlerImpl() {

    private val args = DAppDetailsDialogArgs(savedStateHandle)
    private lateinit var authorizedDapp: AuthorizedDapp

    override fun initialState(): State = State(
        dAppDefinitionAddress = args.dAppDefinitionAddress
    )

    init {
        viewModelScope.launch {
            getDAppWithResourcesUseCase(
                definitionAddress = args.dAppDefinitionAddress,
                needMostRecentData = false
            ).onSuccess { dAppWithResources ->
                _state.update { it.copy(dAppWithResources = dAppWithResources) }
            }.onFailure { error ->
                _state.update { it.copy(uiMessage = UiMessage.ErrorMessage(error)) }
            }.map { dAppWithResources ->
                _state.update { it.copy(isWebsiteValidating = true) }
                getValidatedDAppWebsiteUseCase(dAppWithResources.dApp).onSuccess { website ->
                    _state.update { it.copy(validatedWebsite = website, isWebsiteValidating = false) }
                }.onFailure { error ->
                    Timber.w(error)
                    _state.update { it.copy(isWebsiteValidating = false) }
                }
            }
        }
        observeDapp()
    }

    fun onDeleteDapp() {
        viewModelScope.launch {
            dAppConnectionRepository.deleteAuthorizedDApp(args.dAppDefinitionAddress)
            sendEvent(Event.DAppDeleted)
        }
    }

    private fun observeDapp() {
        viewModelScope.launch {
            dAppConnectionRepository.getAuthorizedDAppFlow(args.dAppDefinitionAddress)
                .filterNotNull()
                .collect { authorizedDapp ->
                    this@DAppDetailsDialogViewModel.authorizedDapp = authorizedDapp
                    val personas = authorizedDapp.referencesToAuthorizedPersonas.mapNotNull { personaSimple ->
                        getProfileUseCase().activePersonaOnCurrentNetwork(personaSimple.identityAddress)
                    }
                    _state.update { state ->
                        state.copy(
                            authorizedPersonas = personas.toPersistentList(),
                        )
                    }
                }
        }
    }

    fun onMessageShown() {
        _state.update { it.copy(uiMessage = null) }
    }

    fun onShowLockerDepositsCheckedChange(isChecked: Boolean) {
        viewModelScope.launch {
            _state.update { it.copy(isShowLockerDepositsChecked = isChecked) }
            changeLockerDepositsVisibilityUseCase(authorizedDapp, isChecked)
        }
    }

    data class State(
        val dAppDefinitionAddress: AccountAddress,
        val dAppWithResources: DAppWithResources? = null,
        val validatedWebsite: String? = null,
        val isWebsiteValidating: Boolean = false,
        val authorizedPersonas: ImmutableList<Persona> = persistentListOf(),
        val uiMessage: UiMessage? = null,
        val isShowLockerDepositsChecked: Boolean = false
    ) : UiState

    sealed interface Event : OneOffEvent {
        data object DAppDeleted : Event
    }
}
