package com.babylon.wallet.android.presentation.settings.securitycenter.securityfactors.arculuscard.createpin

import com.babylon.wallet.android.presentation.common.UiMessage
import com.babylon.wallet.android.presentation.common.UiState
import com.babylon.wallet.android.utils.Constants.ARCULUS_PIN_LENGTH

data class CreateArculusPinState(
    val pin: String = "",
    val confirmedPin: String = "",
    val isConfirmButtonLoading: Boolean = false,
    private val uiMessage: UiMessage? = null
) : UiState {

    val isInputComplete: Boolean = pin.length == ARCULUS_PIN_LENGTH &&
        confirmedPin.length == ARCULUS_PIN_LENGTH
    val showPinsNotMatchingError: Boolean = isInputComplete &&
        !pin.equals(confirmedPin, true)
    val isConfirmButtonEnabled: Boolean = isInputComplete && !showPinsNotMatchingError && !isConfirmButtonLoading
    val isConfirmedPinEnabled: Boolean = pin.length == ARCULUS_PIN_LENGTH

    val infoMessage = uiMessage as? UiMessage.InfoMessage
    val errorMessage = uiMessage as? UiMessage.ErrorMessage
}
