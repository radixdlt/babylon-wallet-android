package com.babylon.wallet.android.domain.model

import com.babylon.wallet.android.data.dapp.model.ResetRequestItem
import rdx.works.profile.data.model.pernetwork.OnNetwork.AuthorizedDapp.AuthorizedPersonaSimple.SharedAccounts

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
            val ongoingPersonaRequestItem: PersonaRequestItem? = null,
            val resetRequestItem: ResetRequestItem? = null
        ) : IncomingRequest(requestId, requestMetadata) {

            fun isUsePersonaWithOngoingAccountsOnly(): Boolean {
                return authRequest is AuthRequest.UsePersonaRequest &&
                    ongoingAccountsRequestItem != null && oneTimeAccountsRequestItem == null
            }

            fun isUsePersonaAuth(): Boolean {
                return authRequest is AuthRequest.UsePersonaRequest
            }

            sealed interface AuthRequest {
                data class LoginRequest(val challenge: String? = null) : AuthRequest
                data class UsePersonaRequest(val personaAddress: String) : AuthRequest
            }
        }

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

        data class RequestMetadata(val networkId: Int, val origin: String, val dAppDefinitionAddress: String)

        data class AccountsRequestItem(
            val isOngoing: Boolean,
            val requiresProofOfOwnership: Boolean,
            val numberOfAccounts: Int,
            val quantifier: AccountNumberQuantifier
        ) {
            enum class AccountNumberQuantifier {
                Exactly, AtLeast;

                fun exactly(): Boolean {
                    return this == Exactly
                }
            }
        }

        data class PersonaRequestItem(
            val fields: List<String>,
            val isOngoing: Boolean
        )

        data class ResetRequestItem(
            val accounts: Boolean,
            val personaData: Boolean
        )
    }

    object ParsingError : MessageFromDataChannel

    object None : MessageFromDataChannel

    enum class ConnectionStateChanged : MessageFromDataChannel {
        OPEN, CLOSE, CLOSING, ERROR, CONNECTING, DELETE_CONNECTION
    }
}

fun MessageFromDataChannel.IncomingRequest.AccountsRequestItem.AccountNumberQuantifier.toProfileShareAccountsQuantifier():
    SharedAccounts.NumberOfAccounts.Quantifier {
    return when (this) {
        MessageFromDataChannel.IncomingRequest.AccountsRequestItem.AccountNumberQuantifier.Exactly -> {
            SharedAccounts.NumberOfAccounts.Quantifier.Exactly
        }
        MessageFromDataChannel.IncomingRequest.AccountsRequestItem.AccountNumberQuantifier.AtLeast -> {
            SharedAccounts.NumberOfAccounts.Quantifier.AtLeast
        }
    }
}
