package com.babylon.wallet.android.presentation.dapp

import com.babylon.wallet.android.domain.model.RequiredPersonaFields
import com.radixdlt.sargon.AccountAddress

sealed interface InitialAuthorizedLoginRoute {
    data object CompleteRequest : InitialAuthorizedLoginRoute
    data class SelectPersona(val dappDefinitionAddress: AccountAddress) : InitialAuthorizedLoginRoute
    data class Permission(
        val numberOfAccounts: Int,
        val isExactAccountsCount: Boolean,
        val showBack: Boolean = false,
        val oneTime: Boolean = false
    ) : InitialAuthorizedLoginRoute

    data class OngoingPersonaData(
        val personaAddress: String,
        val requiredPersonaFields: RequiredPersonaFields
    ) : InitialAuthorizedLoginRoute

    data class OneTimePersonaData(
        val requiredPersonaFields: RequiredPersonaFields
    ) : InitialAuthorizedLoginRoute

    data class ChooseAccount(
        val numberOfAccounts: Int,
        val isExactAccountsCount: Boolean,
        val oneTime: Boolean = false,
        val showBack: Boolean = false
    ) : InitialAuthorizedLoginRoute
}
