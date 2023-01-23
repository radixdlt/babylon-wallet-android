package com.babylon.wallet.android.domain.model

sealed interface MessageFromDataChannel {

    sealed class IncomingRequest(val id: String? = null) : MessageFromDataChannel {

        data class AccountsRequest(
            val requestId: String,
            val isOngoing: Boolean,
            val requiresProofOfOwnership: Boolean,
            val numberOfAccounts: Int,
            val quantifier: AccountNumberQuantifier,
            val authRequest: AuthRequest? = null
        ) : IncomingRequest(requestId)

        data class PersonaRequest(
            val requestId: String,
            val fields: List<String>,
            val isOngoing: Boolean,
            val authRequest: AuthRequest? = null
        ) : IncomingRequest(requestId)

        data class TransactionItem(
            val requestId: String,
            val networkId: Int,
            val transactionManifestData: TransactionManifestData,
        ) : IncomingRequest(requestId)

        object Unknown : IncomingRequest()

        object ParsingError : IncomingRequest()

        object None : IncomingRequest()
    }

    sealed interface AuthRequest {
        data class LoginRequest(val challenge: String) : AuthRequest
        data class UsePersonaRequest(val id: String) : AuthRequest
    }

    enum class ConnectionStateChanged : MessageFromDataChannel {
        OPEN, CLOSE, CLOSING, ERROR, CONNECTING
    }

    enum class AccountNumberQuantifier {
        Exactly, AtLEast
    }
}
