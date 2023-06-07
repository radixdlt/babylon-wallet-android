package com.babylon.wallet.android.presentation.dapp

import com.babylon.wallet.android.presentation.dapp.authorized.account.ROUTE_CHOOSE_ACCOUNTS
import com.babylon.wallet.android.presentation.dapp.authorized.permission.ROUTE_DAPP_PERMISSION
import com.babylon.wallet.android.presentation.dapp.authorized.personaonetime.ROUTE_PERSONA_DATA_ONETIME_AUTHORIZED
import com.babylon.wallet.android.presentation.dapp.authorized.personaongoing.ROUTE_PERSONA_DATA_ONGOING
import com.babylon.wallet.android.presentation.dapp.authorized.selectpersona.ROUTE_SELECT_PERSONA

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
        val requestedFieldsEncoded: String
    ) : InitialAuthorizedLoginRoute

    data class OneTimePersonaData(
        val requestedFieldsEncoded: String
    ) : InitialAuthorizedLoginRoute

    data class ChooseAccount(
        val numberOfAccounts: Int,
        val isExactAccountsCount: Boolean,
        val oneTime: Boolean = false,
        val showBack: Boolean = false
    ) : InitialAuthorizedLoginRoute

    fun toRouteString(): String {
        return when (this) {
            is ChooseAccount -> ROUTE_CHOOSE_ACCOUNTS
            is Permission -> ROUTE_DAPP_PERMISSION
            is SelectPersona -> ROUTE_SELECT_PERSONA
            is OngoingPersonaData -> ROUTE_PERSONA_DATA_ONGOING
            is OneTimePersonaData -> ROUTE_PERSONA_DATA_ONETIME_AUTHORIZED
        }
    }
}
