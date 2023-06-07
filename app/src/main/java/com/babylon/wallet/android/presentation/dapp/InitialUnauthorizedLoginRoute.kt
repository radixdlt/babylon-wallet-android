package com.babylon.wallet.android.presentation.dapp

import com.babylon.wallet.android.presentation.dapp.unauthorized.accountonetime.ROUTE_CHOOSE_ACCOUNTS_ONETIME
import com.babylon.wallet.android.presentation.dapp.unauthorized.personaonetime.ROUTE_PERSONA_DATA_ONETIME_UNAUTHORIZED

sealed interface InitialUnauthorizedLoginRoute {
    data class OnetimePersonaData(
        val requestedFieldsEncoded: String
    ) : InitialUnauthorizedLoginRoute

    data class ChooseAccount(
        val numberOfAccounts: Int,
        val isExactAccountsCount: Boolean
    ) : InitialUnauthorizedLoginRoute

    fun toRouteString(): String {
        return when (this) {
            is ChooseAccount -> ROUTE_CHOOSE_ACCOUNTS_ONETIME
            is OnetimePersonaData -> ROUTE_PERSONA_DATA_ONETIME_UNAUTHORIZED
        }
    }
}
