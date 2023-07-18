package com.babylon.wallet.android.presentation.dapp

import com.babylon.wallet.android.domain.model.RequiredFields

sealed interface InitialUnauthorizedLoginRoute {
    data class OnetimePersonaData(
        val requiredFields: RequiredFields
    ) : InitialUnauthorizedLoginRoute

    data class ChooseAccount(
        val numberOfAccounts: Int,
        val isExactAccountsCount: Boolean
    ) : InitialUnauthorizedLoginRoute
}
