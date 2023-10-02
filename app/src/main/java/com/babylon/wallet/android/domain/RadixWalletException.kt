package com.babylon.wallet.android.domain

import androidx.annotation.StringRes
import com.babylon.wallet.android.R

sealed class RadixWalletException(msg: String? = null) : Exception(msg.orEmpty()) {

    data object FailedToCollectLedgerSignature : RadixWalletException()
    data object ErrorParsingIncomingRequest : RadixWalletException()
    data object ErrorParsingLedgerResponse : RadixWalletException()

    @StringRes
    fun toDescriptionRes(): Int {
        return when (this) {
            FailedToCollectLedgerSignature -> R.string.common_somethingWentWrong // TODO consider different copy
            ErrorParsingIncomingRequest -> R.string.common_somethingWentWrong // TODO consider different copy
            ErrorParsingLedgerResponse -> R.string.common_somethingWentWrong // TODO consider different copy
        }
    }
}
