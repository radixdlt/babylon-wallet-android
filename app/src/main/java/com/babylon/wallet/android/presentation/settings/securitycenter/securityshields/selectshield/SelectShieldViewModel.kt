package com.babylon.wallet.android.presentation.settings.securitycenter.securityshields.selectshield

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.babylon.wallet.android.domain.model.Selectable
import com.babylon.wallet.android.domain.usecases.securityshields.GetSecurityShieldCardsUseCase
import com.babylon.wallet.android.presentation.common.OneOffEvent
import com.babylon.wallet.android.presentation.common.OneOffEventHandler
import com.babylon.wallet.android.presentation.common.OneOffEventHandlerImpl
import com.babylon.wallet.android.presentation.common.StateViewModel
import com.babylon.wallet.android.presentation.common.UiMessage
import com.babylon.wallet.android.presentation.common.UiState
import com.babylon.wallet.android.presentation.ui.model.securityshields.SecurityShieldCard
import com.radixdlt.sargon.AddressOfAccountOrPersona
import com.radixdlt.sargon.SecurityStructureId
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import rdx.works.profile.domain.GetProfileUseCase
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class SelectShieldViewModel @Inject constructor(
    private val getSecurityShieldCardsUseCase: GetSecurityShieldCardsUseCase,
    getProfileUseCase: GetProfileUseCase,
    savedStateHandle: SavedStateHandle
) : StateViewModel<SelectShieldViewModel.State>(),
    OneOffEventHandler<SelectShieldViewModel.Event> by OneOffEventHandlerImpl() {

    val args = ApplyShieldToEntityArgs(savedStateHandle)

    init {
        getProfileUseCase.flow
            .map { it.appPreferences.security.securityStructuresOfFactorSourceIds }
            .distinctUntilChanged()
            .onEach { shieldIds ->
                val currentIds = state.value.shields.map { it.data.id }
                val updatedIds = shieldIds.map { it.metadata.id }
                val newShieldId = updatedIds.subtract(currentIds).firstOrNull()
                initSecurityShields(newShieldId)
            }
            .launchIn(viewModelScope)
    }

    private fun initSecurityShields(selectedId: SecurityStructureId?) {
        viewModelScope.launch {
            getSecurityShieldCardsUseCase()
                .onFailure { error ->
                    Timber.e("Failed to get security shields for display: $error")
                    _state.update { state ->
                        state.copy(errorMessage = UiMessage.ErrorMessage(error))
                    }
                }.onSuccess { shields ->
                    _state.update { state ->
                        state.copy(
                            shields = shields.map {
                                Selectable(
                                    data = it,
                                    selected = it.id == selectedId
                                )
                            },
                            isLoading = false
                        )
                    }
                }
        }
    }

    override fun initialState(): State = State(isLoading = true)

    fun onShieldClick(id: SecurityStructureId) {
        _state.update { state ->
            state.copy(
                shields = state.shields.map { shield ->
                    shield.copy(selected = shield.data.id == id)
                }
            )
        }
    }

    fun onConfirmClick() {
        viewModelScope.launch {
            sendEvent(
                Event.Complete(
                    securityStructureId = requireNotNull(state.value.selectedId),
                    entityAddress = args.address
                )
            )
        }
    }

    fun onDismissMessage() {
        _state.update { state -> state.copy(errorMessage = null) }
    }

    sealed interface Event : OneOffEvent {

        data class Complete(
            val securityStructureId: SecurityStructureId,
            val entityAddress: AddressOfAccountOrPersona
        ) : Event
    }

    data class State(
        val isLoading: Boolean,
        val shields: List<Selectable<SecurityShieldCard>> = persistentListOf(),
        val errorMessage: UiMessage.ErrorMessage? = null
    ) : UiState {

        val selectedId = shields.firstOrNull { it.selected }?.data?.id
        val isButtonEnabled = selectedId != null
    }
}
