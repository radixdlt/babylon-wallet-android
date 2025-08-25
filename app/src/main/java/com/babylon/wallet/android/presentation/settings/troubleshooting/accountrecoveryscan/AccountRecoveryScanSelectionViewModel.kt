package com.babylon.wallet.android.presentation.settings.troubleshooting.accountrecoveryscan

import androidx.lifecycle.viewModelScope
import com.babylon.wallet.android.presentation.common.OneOffEvent
import com.babylon.wallet.android.presentation.common.OneOffEventHandler
import com.babylon.wallet.android.presentation.common.OneOffEventHandlerImpl
import com.babylon.wallet.android.presentation.common.StateViewModel
import com.babylon.wallet.android.presentation.common.UiState
import com.babylon.wallet.android.presentation.selectfactorsource.SelectFactorSourceInput
import com.babylon.wallet.android.presentation.selectfactorsource.SelectFactorSourceProxy
import com.radixdlt.sargon.FactorSourceId
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import rdx.works.core.sargon.isCurrentNetworkMainnet
import rdx.works.profile.domain.GetProfileUseCase
import javax.inject.Inject

@HiltViewModel
class AccountRecoveryScanSelectionViewModel @Inject constructor(
    private val getProfileUseCase: GetProfileUseCase,
    private val selectFactorSourceProxy: SelectFactorSourceProxy
) : StateViewModel<AccountRecoveryScanSelectionViewModel.State>(),
    OneOffEventHandler<AccountRecoveryScanSelectionViewModel.Event> by OneOffEventHandlerImpl() {

    init {
        viewModelScope.launch {
            getProfileUseCase.flow.collect { profile ->
                _state.update { it.copy(isMainnet = profile.isCurrentNetworkMainnet) }
            }
        }
    }

    fun onBackClick() {
        viewModelScope.launch { sendEvent(Event.OnDismiss) }
    }

    fun onRecoverBabylonAccounts() {
        viewModelScope.launch {
            val factorSourceId = selectFactorSource(false) ?: return@launch

            sendEvent(
                Event.FactorSourceSelected(
                    factorSourceId = factorSourceId,
                    isForOlympia = false
                )
            )
        }
    }

    fun onRecoverOlympiaAccounts() {
        viewModelScope.launch {
            val factorSourceId = selectFactorSource(true) ?: return@launch
            sendEvent(
                Event.FactorSourceSelected(
                    factorSourceId = factorSourceId,
                    isForOlympia = true
                )
            )
        }
    }

    private suspend fun selectFactorSource(isOlympia: Boolean): FactorSourceId.Hash? =
        selectFactorSourceProxy.selectFactorSource(
            context = SelectFactorSourceInput.Context.AccountRecovery(
                isOlympia = isOlympia
            )
        )?.value as? FactorSourceId.Hash

    data class State(
        val isMainnet: Boolean = false
    ) : UiState

    sealed interface Event : OneOffEvent {

        data object OnDismiss : Event

        data class FactorSourceSelected(
            val factorSourceId: FactorSourceId.Hash,
            val isForOlympia: Boolean
        ) : Event
    }

    override fun initialState(): State {
        return State()
    }
}
