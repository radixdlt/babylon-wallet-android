package com.babylon.wallet.android.domain

import androidx.annotation.StringRes
import com.babylon.wallet.android.R

sealed class RadixWalletException(msg: String? = null, cause: Throwable? = null) : Throwable(message = msg.orEmpty(), cause = cause) {

    data object FailedToCollectLedgerSignature : RadixWalletException()
    data object FailedToCollectSigners : RadixWalletException("Failed to find signers for the transaction")
    class ErrorParsingIncomingRequest(cause: Throwable?) : RadixWalletException(cause = cause)
    class ErrorParsingLedgerResponse(cause: Throwable?) : RadixWalletException(cause = cause)

    @StringRes
    fun toDescriptionRes(): Int {
        return when (this) {
            FailedToCollectLedgerSignature -> R.string.common_somethingWentWrong // TODO consider different copy
            is ErrorParsingIncomingRequest -> R.string.common_somethingWentWrong // TODO consider different copy
            is ErrorParsingLedgerResponse -> R.string.common_somethingWentWrong // TODO consider different copy
            FailedToCollectSigners -> R.string.common_somethingWentWrong // TODO consider different copy
        }
    }
}
