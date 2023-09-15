package com.babylon.wallet.android.data.transaction

import androidx.annotation.StringRes
import com.babylon.wallet.android.R

@Suppress("CyclomaticComplexMethod")
sealed class RadixWalletException(msg: String? = null) : Exception(msg.orEmpty()) {

    data object DappMetadataEmpty : RadixWalletException()

    @StringRes
    fun toDescriptionRes(): Int {
        return when (this) {
            DappMetadataEmpty -> R.string.common_somethingWentWrong // TODO consider different copy
        }
    }
}
