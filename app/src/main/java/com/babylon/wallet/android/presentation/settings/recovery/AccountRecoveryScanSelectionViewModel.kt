package com.babylon.wallet.android.presentation.settings.recovery

import androidx.lifecycle.viewModelScope
import com.babylon.wallet.android.presentation.common.OneOffEvent
import com.babylon.wallet.android.presentation.common.OneOffEventHandler
import com.babylon.wallet.android.presentation.common.OneOffEventHandlerImpl
import com.babylon.wallet.android.presentation.common.StateViewModel
import com.babylon.wallet.android.presentation.common.UiState
import com.babylon.wallet.android.presentation.onboarding.restore.mnemonic.MnemonicType
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import rdx.works.profile.data.model.extensions.isCurrentNetworkMainnet
import rdx.works.profile.domain.GetProfileUseCase
import javax.inject.Inject

@HiltViewModel
class AccountRecoveryScanSelectionViewModel @Inject constructor(
    private val getProfileUseCase: GetProfileUseCase
) : StateViewModel<AccountRecoveryScanSelectionViewModel.State>(),
    OneOffEventHandler<AccountRecoveryScanSelectionViewModel.Event> by OneOffEventHandlerImpl() {

    init {
        viewModelScope.launch {
            getProfileUseCase().collect { profile ->
                _state.update { it.copy(isMainnet = profile.isCurrentNetworkMainnet()) }
            }
        }
    }

    fun onBackClick() {
        viewModelScope.launch { sendEvent(Event.OnDismiss) }
    }

    fun onUseFactorSource(recoveryType: RecoveryType) {
        viewModelScope.launch {
            when (recoveryType) {
                RecoveryType.DeviceBabylon -> sendEvent(Event.ChooseSeedPhrase(MnemonicType.Babylon))
                RecoveryType.DeviceOlympia -> sendEvent(Event.ChooseSeedPhrase(MnemonicType.Olympia))
                RecoveryType.LedgerBabylon -> sendEvent(Event.ChooseLedger(false))
                RecoveryType.LedgerOlympia -> sendEvent(Event.ChooseLedger(true))
            }
        }
    }

    data class State(
        val isMainnet: Boolean = false
    ) : UiState

    sealed interface Event : OneOffEvent {
        data object OnDismiss : Event
        data class ChooseSeedPhrase(val mnemonicType: MnemonicType) : Event
        data class ChooseLedger(val isOlympia: Boolean) : Event
    }

    override fun initialState(): State {
        return State()
    }
}

enum class RecoveryType {
    DeviceBabylon, DeviceOlympia, LedgerBabylon, LedgerOlympia
}
