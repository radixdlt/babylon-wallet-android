package com.babylon.wallet.android.presentation.dapp.unauthorized

import com.babylon.wallet.android.domain.model.messages.RequiredPersonaFields

sealed interface InitialUnauthorizedLoginRoute {
    data class OnetimePersonaData(
        val requiredPersonaFields: RequiredPersonaFields
    ) : InitialUnauthorizedLoginRoute

    data class ChooseAccount(
        val numberOfAccounts: Int,
        val isExactAccountsCount: Boolean
    ) : InitialUnauthorizedLoginRoute
}
