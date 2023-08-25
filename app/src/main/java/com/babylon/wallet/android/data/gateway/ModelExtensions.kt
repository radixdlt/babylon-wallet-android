package com.babylon.wallet.android.data.gateway

import com.babylon.wallet.android.data.gateway.generated.models.TransactionStatus

fun TransactionStatus.isComplete(): Boolean {
    return listOf(
        TransactionStatus.committedSuccess,
        TransactionStatus.committedFailure,
        TransactionStatus.rejected
    ).contains(this)
}

fun TransactionStatus.isFailed(): Boolean {
    return listOf(
        TransactionStatus.committedFailure,
        TransactionStatus.rejected
    ).contains(this)
}
