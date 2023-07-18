package com.babylon.wallet.android.presentation.dapp

import com.babylon.wallet.android.domain.model.RequiredFields

sealed interface InitialAuthorizedLoginRoute {
    data class SelectPersona(val reqId: String) : InitialAuthorizedLoginRoute
    data class Permission(
        val numberOfAccounts: Int,
        val isExactAccountsCount: Boolean,
        val showBack: Boolean = false,
        val oneTime: Boolean = false
    ) : InitialAuthorizedLoginRoute

    data class OngoingPersonaData(
        val personaAddress: String,
        val requiredFields: RequiredFields
    ) : InitialAuthorizedLoginRoute

    data class OneTimePersonaData(
        val requiredFields: RequiredFields
    ) : InitialAuthorizedLoginRoute

    data class ChooseAccount(
        val numberOfAccounts: Int,
        val isExactAccountsCount: Boolean,
        val oneTime: Boolean = false,
        val showBack: Boolean = false
    ) : InitialAuthorizedLoginRoute
}
