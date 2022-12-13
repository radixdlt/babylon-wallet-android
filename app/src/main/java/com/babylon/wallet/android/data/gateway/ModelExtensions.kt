package com.babylon.wallet.android.data.gateway

import com.babylon.wallet.android.data.gateway.generated.model.ErrorResponse
import com.babylon.wallet.android.data.gateway.generated.model.TransactionStatus

fun ErrorResponse.hasMessageOrDetails(): Boolean {
    return details != null
}

fun TransactionStatus.isComplete(): Boolean {
    return listOf(
        TransactionStatus.committedSuccess,
        TransactionStatus.committedFailure,
        TransactionStatus.rejected
    ).contains(this)
}
