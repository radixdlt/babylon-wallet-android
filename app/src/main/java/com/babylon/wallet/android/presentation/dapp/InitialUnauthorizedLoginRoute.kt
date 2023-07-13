package com.babylon.wallet.android.presentation.dapp

import com.babylon.wallet.android.domain.model.MessageFromDataChannel

sealed interface InitialUnauthorizedLoginRoute {
    data class OnetimePersonaData(
        val request: MessageFromDataChannel.IncomingRequest.PersonaRequestItem
    ) : InitialUnauthorizedLoginRoute

    data class ChooseAccount(
        val numberOfAccounts: Int,
        val isExactAccountsCount: Boolean
    ) : InitialUnauthorizedLoginRoute
}
