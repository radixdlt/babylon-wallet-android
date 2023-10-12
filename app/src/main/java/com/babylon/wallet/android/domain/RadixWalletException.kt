package com.babylon.wallet.android.domain

sealed class RadixWalletException(msg: String? = null, cause: Throwable? = null) : Throwable(message = msg.orEmpty(), cause = cause) {
    data object FailedToCollectSigners : RadixWalletException("Failed to collect signers for the transaction")
    data object DappMetadataEmpty : RadixWalletException("Empty dApp metadata")
    data object FailedToCollectLedgerSignature : RadixWalletException()
    class ErrorParsingIncomingRequest(cause: Throwable?) : RadixWalletException(cause = cause)
    class ErrorParsingLedgerResponse(cause: Throwable?) : RadixWalletException(cause = cause)
}
