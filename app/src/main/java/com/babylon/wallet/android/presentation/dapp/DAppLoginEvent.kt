package com.babylon.wallet.android.presentation.dapp

import com.babylon.wallet.android.domain.model.RequiredPersonaFields
import com.babylon.wallet.android.presentation.common.OneOffEvent

sealed interface DAppLoginEvent : OneOffEvent {

    data object CloseLoginFlow : DAppLoginEvent
    data class RequestCompletionBiometricPrompt(val isSignatureRequired: Boolean) : DAppLoginEvent

    data object LoginFlowCompleted : DAppLoginEvent

    data class DisplayPermission(
        val numberOfAccounts: Int,
        val isExactAccountsCount: Boolean,
        val oneTime: Boolean = false
    ) : DAppLoginEvent

    data class PersonaDataOngoing(
        val personaAddress: String,
        val requiredPersonaFields: RequiredPersonaFields
    ) : DAppLoginEvent

    data class PersonaDataOnetime(val requiredPersonaFields: RequiredPersonaFields) : DAppLoginEvent

    data class ChooseAccounts(
        val numberOfAccounts: Int,
        val isExactAccountsCount: Boolean,
        val oneTime: Boolean = false,
        val showBack: Boolean = true
    ) : DAppLoginEvent
}