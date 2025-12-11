package com.babylon.wallet.android.presentation.settings.securitycenter.applyshield.apply

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.babylon.wallet.android.data.dapp.IncomingRequestRepository
import com.babylon.wallet.android.di.coroutines.DefaultDispatcher
import com.babylon.wallet.android.domain.usecases.transaction.PrepareApplyShieldRequestUseCase
import com.babylon.wallet.android.presentation.common.OneOffEvent
import com.babylon.wallet.android.presentation.common.OneOffEventHandler
import com.babylon.wallet.android.presentation.common.OneOffEventHandlerImpl
import com.babylon.wallet.android.presentation.common.StateViewModel
import com.babylon.wallet.android.presentation.common.UiMessage
import com.babylon.wallet.android.presentation.common.UiState
import com.babylon.wallet.android.presentation.settings.securitycenter.applyshield.ApplyShieldArgs
import com.babylon.wallet.android.presentation.ui.model.factors.FactorSourceCard
import com.babylon.wallet.android.presentation.ui.model.factors.toFactorSourceCard
import com.babylon.wallet.android.utils.callSafely
import com.radixdlt.sargon.AddressOfAccountOrPersona
import com.radixdlt.sargon.SecurityStructureOfFactorSources
import com.radixdlt.sargon.os.SargonOsManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ApplyShieldViewModel @Inject constructor(
    private val prepareApplyShieldRequestUseCase: PrepareApplyShieldRequestUseCase,
    private val incomingRequestRepository: IncomingRequestRepository,
    private val sargonOsManager: SargonOsManager,
    @DefaultDispatcher private val dispatcher: CoroutineDispatcher,
    savedStateHandle: SavedStateHandle
) : StateViewModel<ApplyShieldViewModel.State>(),
    OneOffEventHandler<ApplyShieldViewModel.Event> by OneOffEventHandlerImpl() {

    private val args = ApplyShieldArgs(savedStateHandle)
    private lateinit var securityStructure: SecurityStructureOfFactorSources

    init {
        viewModelScope.launch {
            sargonOsManager.callSafely(dispatcher) {
                securityStructure = sargonOsManager.sargonOs.securityStructuresOfFactorSources()
                    .first { it.metadata.id == args.securityStructureId }
                _state.update { state ->
                    state.copy(
                        isLoading = false,
                        factors = sortedFactorSourcesFromSecurityStructure(securityStructure)
                            .map { it.toFactorSourceCard(includeLastUsedOn = false) }
                            .toPersistentList()
                    )
                }
            }
        }
    }

    override fun initialState(): State = State(isLoading = true)

    fun onApplyClick(
        entityAddress: AddressOfAccountOrPersona
    ) = viewModelScope.launch {
        _state.update { state -> state.copy(isApplyLoading = true) }

        prepareApplyShieldRequestUseCase.applyShield(args.securityStructureId, entityAddress)
            .onFailure { error ->
                _state.update { state ->
                    state.copy(
                        isApplyLoading = false,
                        message = UiMessage.ErrorMessage(error)
                    )
                }
            }.onSuccess { request ->
                _state.update { state -> state.copy(isApplyLoading = false) }
                sendEvent(Event.ShieldApplied)

                incomingRequestRepository.add(request)
            }
    }

    fun onMessageShown() {
        _state.update { state -> state.copy(message = null) }
    }

    data class State(
        val isLoading: Boolean,
        val isApplyLoading: Boolean = false,
        val message: UiMessage? = null,
        val factors: ImmutableList<FactorSourceCard> = persistentListOf()
    ) : UiState

    sealed interface Event : OneOffEvent {

        data object ShieldApplied : Event
    }
}
