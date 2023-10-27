package com.babylon.wallet.android.utils

import com.babylon.wallet.android.domain.RadixWalletException
import com.babylon.wallet.android.domain.asRadixWalletException

inline fun <T> Result<T>.onFailureWithRadixException(action: (exception: RadixWalletException) -> Unit): Result<T> {
    exceptionOrNull()?.asRadixWalletException()?.let { action(it) }
    return this
}
