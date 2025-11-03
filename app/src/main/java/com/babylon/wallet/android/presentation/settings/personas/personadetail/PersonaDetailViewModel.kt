package com.babylon.wallet.android.presentation.settings.personas.personadetail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.babylon.wallet.android.domain.usecases.GetDAppsUseCase
import com.babylon.wallet.android.domain.usecases.factorsources.GetFactorSourceIntegrityStatusMessagesUseCase
import com.babylon.wallet.android.domain.utils.AccessControllerTimedRecoveryStateObserver
import com.babylon.wallet.android.presentation.common.OneOffEvent
import com.babylon.wallet.android.presentation.common.OneOffEventHandler
import com.babylon.wallet.android.presentation.common.OneOffEventHandlerImpl
import com.babylon.wallet.android.presentation.common.StateViewModel
import com.babylon.wallet.android.presentation.common.UiState
import com.babylon.wallet.android.presentation.common.secured.SecuredWithUiData
import com.babylon.wallet.android.presentation.ui.model.factors.toFactorSourceCard
import com.radixdlt.sargon.AddressOfAccountOrPersona
import com.radixdlt.sargon.EntitySecurityState
import com.radixdlt.sargon.NetworkId
import com.radixdlt.sargon.Persona
import com.radixdlt.sargon.extensions.asGeneral
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import rdx.works.core.domain.DApp
import rdx.works.core.sargon.activePersonaOnCurrentNetwork
import rdx.works.core.sargon.currentNetwork
import rdx.works.core.sargon.factorSourceById
import rdx.works.profile.data.repository.DAppConnectionRepository
import rdx.works.profile.domain.ChangeEntityVisibilityUseCase
import rdx.works.profile.domain.GetProfileUseCase
import javax.inject.Inject

@Suppress("LongParameterList")
@HiltViewModel
class PersonaDetailViewModel @Inject constructor(
    dAppConnectionRepository: DAppConnectionRepository,
    getProfileUseCase: GetProfileUseCase,
    private val getDAppsUseCase: GetDAppsUseCase,
    savedStateHandle: SavedStateHandle,
    private val changeEntityVisibilityUseCase: ChangeEntityVisibilityUseCase,
    private val getFactorSourceIntegrityStatusMessagesUseCase: GetFactorSourceIntegrityStatusMessagesUseCase,
    private val timedRecoveryStateObserver: AccessControllerTimedRecoveryStateObserver
) : StateViewModel<PersonaDetailViewModel.State>(),
    OneOffEventHandler<PersonaDetailViewModel.Event> by OneOffEventHandlerImpl() {

    private val args = PersonaDetailScreenArgs(savedStateHandle)
    private var recoveryStateJob: Job? = null

    init {
        viewModelScope.launch {
            combine(
                getProfileUseCase.flow.mapNotNull { profile ->
                    profile.activePersonaOnCurrentNetwork(args.personaAddress)?.let { persona ->
                        val securedWith = when (val securityState = persona.securityState) {
                            is EntitySecurityState.Securified -> SecuredWithUiData.Shield(
                                isInTimedRecovery = timedRecoveryStateObserver.cachedStateByAddress(
                                    address = AddressOfAccountOrPersona.Identity(args.personaAddress)
                                )?.timedRecoveryState != null
                            )
                            is EntitySecurityState.Unsecured -> profile.factorSourceById(
                                id = securityState.value.transactionSigning.factorSourceId.asGeneral()
                            )?.let { factorSource ->
                                SecuredWithUiData.Factor(
                                    factorSourceCard = factorSource.toFactorSourceCard(
                                        includeLastUsedOn = true,
                                        messages = getFactorSourceIntegrityStatusMessagesUseCase.forFactorSource(
                                            factorSource = factorSource,
                                            includeNoIssuesStatus = false,
                                            checkIntegrityOnlyIfAnyEntitiesLinked = false
                                        ).toPersistentList()
                                    )
                                )
                            }
                        }
                        persona to securedWith
                    }
                },
                dAppConnectionRepository.getAuthorizedDAppsByPersona(args.personaAddress)
            ) { personaAndSecuredWith, dApps ->
                personaAndSecuredWith to dApps
            }.collect { personaAndSecuredWithToDApps ->
                val personaAndSecuredWith = personaAndSecuredWithToDApps.first
                val metadataResults = personaAndSecuredWithToDApps.second.map { authorizedDApp ->
                    getDAppsUseCase.invoke(
                        definitionAddress = authorizedDApp.dappDefinitionAddress,
                        needMostRecentData = false
                    ).getOrNull()
                }
                val dApps = metadataResults.mapNotNull { dAppWithAssociatedResources ->
                    dAppWithAssociatedResources
                }
                _state.update { state ->
                    state.copy(
                        authorizedDapps = dApps.toImmutableList(),
                        persona = personaAndSecuredWith.first,
                        securedWith = personaAndSecuredWith.second,
                        loading = false
                    )
                }

                observeRecoveryState()
            }
        }

        getProfileUseCase.flow.map { it.currentNetwork?.id == NetworkId.STOKENET }
            .onEach { isOnStokenet ->
                _state.update { state -> state.copy(isMfaEnabled = isOnStokenet) }
            }.launchIn(viewModelScope)
    }

    override fun initialState(): State {
        return State()
    }

    fun onHidePersona() {
        viewModelScope.launch {
            state.value.persona?.address?.let {
                changeEntityVisibilityUseCase.changePersonaVisibility(
                    entityAddress = it,
                    hide = true
                )
            }
            sendEvent(Event.Close)
        }
    }

    private fun observeRecoveryState() {
        val personaAddress = AddressOfAccountOrPersona.Identity(args.personaAddress)
        recoveryStateJob?.cancel()
        recoveryStateJob = timedRecoveryStateObserver.recoveryStateByAddress
            .mapNotNull { states -> states[personaAddress] }
            .onEach { recoveryState ->
                val securedWith = _state.value.securedWith as? SecuredWithUiData.Shield ?: return@onEach

                _state.update { state ->
                    state.copy(
                        securedWith = securedWith.copy(
                            isInTimedRecovery = recoveryState.timedRecoveryState != null
                        )
                    )
                }
            }
            .launchIn(viewModelScope)
    }

    sealed interface Event : OneOffEvent {
        data object Close : Event
    }

    data class State(
        val loading: Boolean = true,
        val authorizedDapps: ImmutableList<DApp> = persistentListOf(),
        val persona: Persona? = null,
        val isMfaEnabled: Boolean = false,
        val securedWith: SecuredWithUiData? = null
    ) : UiState {

        val canApplyShield
            get() = isMfaEnabled && securedWith !is SecuredWithUiData.Shield
        val address
            get() = AddressOfAccountOrPersona.Identity(requireNotNull(persona?.address))
    }
}
