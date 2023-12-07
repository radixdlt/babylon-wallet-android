package com.babylon.wallet.android.presentation.settings.recovery

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.babylon.wallet.android.presentation.common.OneOffEvent
import com.babylon.wallet.android.presentation.common.OneOffEventHandler
import com.babylon.wallet.android.presentation.common.OneOffEventHandlerImpl
import com.babylon.wallet.android.presentation.common.UiMessage
import com.babylon.wallet.android.presentation.common.UiState
import com.babylon.wallet.android.presentation.onboarding.restore.mnemonic.MnemonicType
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DeriveLegacyAccountViewModel @Inject constructor() :
    ViewModel(),
    OneOffEventHandler<DeriveLegacyAccountViewModel.Event> by OneOffEventHandlerImpl() {

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
        val showLedgerOrOlympiaPrompt: Boolean = false,
        val uiMessage: UiMessage? = null
    ) : UiState

    sealed interface Event : OneOffEvent {
        data object OnDismiss : Event
        data class ChooseSeedPhrase(val mnemonicType: MnemonicType) : Event
        data class ChooseLedger(val isOlympia: Boolean) : Event
    }
}

enum class RecoveryType {
    DeviceBabylon, DeviceOlympia, LedgerBabylon, LedgerOlympia
}
