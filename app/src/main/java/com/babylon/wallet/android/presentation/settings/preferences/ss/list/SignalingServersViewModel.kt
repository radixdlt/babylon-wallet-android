package com.babylon.wallet.android.presentation.settings.preferences.ss.list

import androidx.lifecycle.viewModelScope
import com.babylon.wallet.android.di.coroutines.DefaultDispatcher
import com.babylon.wallet.android.presentation.common.StateViewModel
import com.babylon.wallet.android.presentation.common.UiMessage
import com.babylon.wallet.android.presentation.common.UiState
import com.babylon.wallet.android.utils.callSafely
import com.radixdlt.sargon.P2pTransportProfile
import com.radixdlt.sargon.os.SargonOsManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import rdx.works.profile.domain.GetProfileUseCase
import javax.inject.Inject

@HiltViewModel
class SignalingServersViewModel @Inject constructor(
    getProfileUseCase: GetProfileUseCase,
    private val sargonOsManager: SargonOsManager,
    @DefaultDispatcher private val defaultDispatcher: CoroutineDispatcher
) : StateViewModel<SignalingServersViewModel.State>() {

    init {
        getProfileUseCase.flow.onEach {
            loadServers()
        }.launchIn(viewModelScope)
    }

    override fun initialState(): State = State()

    fun onDismissErrorMessage() {
        _state.update { state ->
            state.copy(
                errorMessage = null
            )
        }
    }

    private fun loadServers() {
        viewModelScope.launch {
            sargonOsManager.callSafely(defaultDispatcher) {
                p2pTransportProfiles()
            }.onFailure {
                onError(it)
            }.onSuccess { p2pTransportProfiles ->
                _state.update { state ->
                    state.copy(
                        current = p2pTransportProfiles.current,
                        items = p2pTransportProfiles.other
                    )
                }
            }
        }
    }

    private fun onError(throwable: Throwable) {
        _state.update { state ->
            state.copy(
                errorMessage = UiMessage.ErrorMessage(throwable)
            )
        }
    }

    data class State(
        val current: P2pTransportProfile? = null,
        val items: List<P2pTransportProfile> = emptyList(),
        val errorMessage: UiMessage.ErrorMessage? = null
    ) : UiState
}
