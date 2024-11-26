package com.babylon.wallet.android.presentation.dapp.authorized

import com.babylon.wallet.android.domain.model.messages.RequiredPersonaFields
import com.babylon.wallet.android.presentation.dapp.authorized.verifyentities.EntitiesForProofWithSignatures
import com.radixdlt.sargon.AccountAddress
import com.radixdlt.sargon.IdentityAddress

sealed interface InitialAuthorizedLoginRoute {

    data object CompleteRequest : InitialAuthorizedLoginRoute

    data class SelectPersona(
        val authorizedRequestInteractionId: String,
        val dappDefinitionAddress: AccountAddress
    ) : InitialAuthorizedLoginRoute

    data class OngoingAccounts(
        val authorizedRequestInteractionId: String,
        val isOneTimeRequest: Boolean = false,
        val isExactAccountsCount: Boolean,
        val numberOfAccounts: Int,
        val showBack: Boolean = false
    ) : InitialAuthorizedLoginRoute

    data class OngoingPersonaData(
        val personaAddress: IdentityAddress,
        val requiredPersonaFields: RequiredPersonaFields
    ) : InitialAuthorizedLoginRoute

    data class OneTimePersonaData(
        val requiredPersonaFields: RequiredPersonaFields
    ) : InitialAuthorizedLoginRoute

    data class OneTimeAccounts(
        val authorizedRequestInteractionId: String,
        val isOneTimeRequest: Boolean = false,
        val isExactAccountsCount: Boolean,
        val numberOfAccounts: Int,
        val showBack: Boolean = false
    ) : InitialAuthorizedLoginRoute

    data class VerifyPersona(
        val walletAuthorizedRequestInteractionId: String,
        val entitiesForProofWithSignatures: EntitiesForProofWithSignatures
    ) : InitialAuthorizedLoginRoute

    data class VerifyAccounts(
        val walletAuthorizedRequestInteractionId: String,
        val entitiesForProofWithSignatures: EntitiesForProofWithSignatures
    ) : InitialAuthorizedLoginRoute
}
