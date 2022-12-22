package com.babylon.wallet.android.domain.model

sealed interface MessageFromDataChannel {

    sealed interface IncomingRequest : MessageFromDataChannel {

        data class AccountsRequest(
            val requestId: String,
            val isOngoing: Boolean,
            val requiresProofOfOwnership: Boolean,
            val numberOfAccounts: Int
        ) : IncomingRequest

        object SomeOtherRequest : IncomingRequest // TODO replace this later with some other request from dapp

        object ParsingError : IncomingRequest

        object None : IncomingRequest
    }

    enum class ConnectionStateChanged : MessageFromDataChannel {
        OPEN, CLOSE, CLOSING, ERROR, CONNECTING
    }
}
