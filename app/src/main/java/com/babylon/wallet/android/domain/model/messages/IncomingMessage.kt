package com.babylon.wallet.android.domain.model.messages

import com.babylon.wallet.android.domain.RadixWalletException

sealed interface IncomingMessage {

    data object ParsingError : IncomingMessage

    data class Error(val exception: RadixWalletException) : IncomingMessage {

        companion object {

            fun invalidRequestChallenge(): Error {
                return Error(RadixWalletException.DappRequestException.InvalidRequestChallenge)
            }
        }
    }
}
