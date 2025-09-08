package com.babylon.wallet.android.presentation.settings.securitycenter.securityshields.applyshield

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
import com.babylon.wallet.android.presentation.ui.model.securityshields.SecurityShieldCard
import com.babylon.wallet.android.utils.callSafely
import com.radixdlt.sargon.SecurityStructureId
import com.radixdlt.sargon.os.SargonOsManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class ApplyShieldToEntityViewModel @Inject constructor(
    private val sargonOsManager: SargonOsManager,
    private val prepareApplyShieldRequestUseCase: PrepareApplyShieldRequestUseCase,
    private val incomingRequestRepository: IncomingRequestRepository,
    @DefaultDispatcher private val defaultDispatcher: CoroutineDispatcher,
    savedStateHandle: SavedStateHandle
) : StateViewModel<ApplyShieldToEntityViewModel.State>(),
    OneOffEventHandler<ApplyShieldToEntityViewModel.Event> by OneOffEventHandlerImpl() {

    private val args = ApplyShieldToEntityArgs(savedStateHandle)

    init {
        viewModelScope.launch {
            getSecurityShields()
        }
    }

    private suspend fun getSecurityShields() {
        sargonOsManager.callSafely(defaultDispatcher) {
            val securityShields = getShieldsForDisplay()

            _state.update { state ->
                state.copy(
                    shields = securityShields.map {
                        SecurityShieldCard(
                            shieldForDisplay = it,
                            messages = persistentListOf()
                        )
                    }.toPersistentList(),
                    isLoading = false
                )
            }
        }.onFailure { error ->
            Timber.e("Failed to get security shields for display: $error")
            _state.update { state ->
                state.copy(errorMessage = UiMessage.ErrorMessage(error))
            }
        }
    }

    override fun initialState(): State = State(isLoading = true)

    fun onShieldClick(id: SecurityStructureId) {
        _state.update { state ->
            state.copy(
                selectedId = id
            )
        }
    }

    fun onConfirmClick() {
        viewModelScope.launch {
            _state.update { state -> state.copy(isApplyLoading = true) }

            prepareApplyShieldRequestUseCase(
                securityStructureId = requireNotNull(state.value.selectedId),
                entityAddress = args.address
            ).onFailure { error ->
                _state.update { state ->
                    state.copy(
                        errorMessage = UiMessage.ErrorMessage(error),
                        isApplyLoading = false
                    )
                }
            }.onSuccess { request ->
                _state.update { state -> state.copy(isApplyLoading = false) }
                sendEvent(Event.Complete)

                incomingRequestRepository.add(request)
            }
        }
    }

    fun onDismissMessage() {
        _state.update { state -> state.copy(errorMessage = null) }
    }

    sealed interface Event : OneOffEvent {

        data object Complete : Event
    }

    data class State(
        val isLoading: Boolean,
        val shields: ImmutableList<SecurityShieldCard> = persistentListOf(),
        val selectedId: SecurityStructureId? = null,
        val isApplyLoading: Boolean = false,
        val errorMessage: UiMessage.ErrorMessage? = null
    ) : UiState {

        val isButtonEnabled = selectedId != null
    }
}
