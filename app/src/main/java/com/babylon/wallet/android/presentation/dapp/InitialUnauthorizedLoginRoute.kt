package com.babylon.wallet.android.presentation.dapp

sealed interface InitialUnauthorizedLoginRoute {
    data class OnetimePersonaData(
        val requestedFieldsEncoded: String
    ) : InitialUnauthorizedLoginRoute

    data class ChooseAccount(
        val numberOfAccounts: Int,
        val isExactAccountsCount: Boolean
    ) : InitialUnauthorizedLoginRoute

}
