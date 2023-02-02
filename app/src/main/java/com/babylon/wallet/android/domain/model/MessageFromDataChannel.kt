package com.babylon.wallet.android.domain.model

import rdx.works.profile.data.model.pernetwork.OnNetwork

sealed interface MessageFromDataChannel {

    sealed class IncomingRequest(val id: String, val metadata: RequestMetadata) :
        MessageFromDataChannel {

        data class AuthorizedRequest(
            val requestId: String,
            val requestMetadata: RequestMetadata,
            val authRequest: AuthRequest,
            val oneTimeAccountsRequestItem: AccountsRequestItem? = null,
            val ongoingAccountsRequestItem: AccountsRequestItem? = null,
            val oneTimePersonaRequestItem: PersonaRequestItem? = null,
            val ongoingPersonaRequestItem: PersonaRequestItem? = null
        ) : IncomingRequest(requestId, requestMetadata)

        data class UnauthorizedRequest(
            val requestId: String,
            val requestMetadata: RequestMetadata,
            val oneTimeAccountsRequestItem: AccountsRequestItem? = null,
            val oneTimePersonaRequestItem: PersonaRequestItem? = null
        ) : IncomingRequest(requestId, requestMetadata)

        data class TransactionRequest(
            val requestId: String,
            val transactionManifestData: TransactionManifestData,
            val requestMetadata: RequestMetadata
        ) : IncomingRequest(requestId, requestMetadata)

        sealed interface AuthRequest {
            data class LoginRequest(val challenge: String? = null) : AuthRequest
            data class UsePersonaRequest(val id: String) : AuthRequest
        }

        enum class AccountNumberQuantifier {
            Exactly, AtLeast
        }

        data class RequestMetadata(val networkId: Int, val origin: String, val dAppDefinitionAddress: String)

        data class AccountsRequestItem(
            val isOngoing: Boolean,
            val requiresProofOfOwnership: Boolean,
            val numberOfAccounts: Int,
            val quantifier: AccountNumberQuantifier
        )

        data class PersonaRequestItem(
            val fields: List<String>,
            val isOngoing: Boolean
        )
    }

    object Unknown : MessageFromDataChannel

    object ParsingError : MessageFromDataChannel

    object None : MessageFromDataChannel

    enum class ConnectionStateChanged : MessageFromDataChannel {
        OPEN, CLOSE, CLOSING, ERROR, CONNECTING
    }
}

fun MessageFromDataChannel.IncomingRequest.AccountNumberQuantifier.toProfileShareAccountsMode(): OnNetwork.ConnectedDapp.AuthorizedPersonaSimple.SharedAccounts.Mode {
    return when (this) {
        MessageFromDataChannel.IncomingRequest.AccountNumberQuantifier.Exactly -> {
            OnNetwork.ConnectedDapp.AuthorizedPersonaSimple.SharedAccounts.Mode.Exactly
        }
        MessageFromDataChannel.IncomingRequest.AccountNumberQuantifier.AtLeast -> {
            OnNetwork.ConnectedDapp.AuthorizedPersonaSimple.SharedAccounts.Mode.AtLeast
        }
    }
}
