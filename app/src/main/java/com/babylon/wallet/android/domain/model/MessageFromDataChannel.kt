package com.babylon.wallet.android.domain.model

sealed interface MessageFromDataChannel {

    sealed class IncomingRequest(val id: String? = null) : MessageFromDataChannel {

        data class AccountsRequest(
            val requestId: String,
            val isOngoing: Boolean,
            val requiresProofOfOwnership: Boolean,
            val numberOfAccounts: Int
        ) : IncomingRequest(requestId)

        data class TransactionWriteRequest(
            val requestId: String,
            val networkId: Int,
            val transactionManifestData: String
        ) : IncomingRequest(requestId)

        object SomeOtherRequest : IncomingRequest() // TODO replace this later with some other request from dapp

        object ParsingError : IncomingRequest()

        object None : IncomingRequest()
    }

    enum class ConnectionStateChanged : MessageFromDataChannel {
        OPEN, CLOSE, CLOSING, ERROR, CONNECTING
    }
}
