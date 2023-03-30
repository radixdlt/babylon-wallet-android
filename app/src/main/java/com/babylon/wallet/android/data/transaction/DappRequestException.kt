package com.babylon.wallet.android.data.transaction

data class DappRequestException(
    val failure: DappRequestFailure,
    val msg: String? = null,
    val e: Throwable? = null
) : Exception(msg, e)
