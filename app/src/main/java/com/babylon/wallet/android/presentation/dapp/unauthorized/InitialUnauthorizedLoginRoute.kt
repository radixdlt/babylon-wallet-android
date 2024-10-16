package com.babylon.wallet.android.presentation.dapp.unauthorized

import com.babylon.wallet.android.domain.model.messages.RequiredPersonaFields
import com.babylon.wallet.android.presentation.dapp.unauthorized.verifyentities.EntitiesForProofWithSignatures

sealed interface InitialUnauthorizedLoginRoute {

    data class OnetimePersonaData(
        val requiredPersonaFields: RequiredPersonaFields
    ) : InitialUnauthorizedLoginRoute

    data class ChooseAccount(
        val numberOfAccounts: Int,
        val isExactAccountsCount: Boolean
    ) : InitialUnauthorizedLoginRoute

    data class VerifyPersona(
        val walletUnauthorizedRequestInteractionId: String,
        val entitiesForProofWithSignatures: EntitiesForProofWithSignatures
    ) : InitialUnauthorizedLoginRoute

    data class VerifyAccounts(
        val walletUnauthorizedRequestInteractionId: String,
        val entitiesForProofWithSignatures: EntitiesForProofWithSignatures
    ) : InitialUnauthorizedLoginRoute
}
