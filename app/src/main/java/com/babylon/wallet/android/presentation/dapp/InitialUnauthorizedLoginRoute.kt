package com.babylon.wallet.android.presentation.dapp

import com.babylon.wallet.android.domain.model.RequiredPersonaFields

sealed interface InitialUnauthorizedLoginRoute {
    data class OnetimePersonaData(
        val requiredPersonaFields: RequiredPersonaFields
    ) : InitialUnauthorizedLoginRoute

    data class ChooseAccount(
        val numberOfAccounts: Int,
        val isExactAccountsCount: Boolean
    ) : InitialUnauthorizedLoginRoute
}
