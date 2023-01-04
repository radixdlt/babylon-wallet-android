package com.babylon.wallet.android.domain.model

sealed interface MessageFromDataChannel {

    sealed class IncomingRequest(val id: String? = null) : MessageFromDataChannel {

        data class AccountsRequest(
            val requestId: String,
            val isOngoing: Boolean,
            val requiresProofOfOwnership: Boolean,
            val numberOfAccounts: Int,
        ) : IncomingRequest(requestId)

        data class TransactionWriteRequest(
            val requestId: String,
            val networkId: Int,
            val transactionManifestData: TransactionManifestData,
        ) : IncomingRequest(requestId)

        object Unknown : IncomingRequest()

        object ParsingError : IncomingRequest()

        object None : IncomingRequest()
    }

    enum class ConnectionStateChanged : MessageFromDataChannel {
        OPEN, CLOSE, CLOSING, ERROR, CONNECTING
    }
}
