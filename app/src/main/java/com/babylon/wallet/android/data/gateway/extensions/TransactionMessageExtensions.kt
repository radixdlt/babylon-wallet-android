package com.babylon.wallet.android.data.gateway.extensions

import com.babylon.wallet.android.data.gateway.coreapi.BinaryPlaintextMessageContent
import com.babylon.wallet.android.data.gateway.coreapi.EncryptedTransactionMessage
import com.babylon.wallet.android.data.gateway.coreapi.PlaintextTransactionMessage
import com.babylon.wallet.android.data.gateway.coreapi.StringPlaintextMessageContent
import com.babylon.wallet.android.data.gateway.coreapi.TransactionMessage

fun TransactionMessage.decode(): String? {
    return when (this) {
        is EncryptedTransactionMessage -> null
        is PlaintextTransactionMessage -> {
            when (content) {
                is StringPlaintextMessageContent -> content.value
                is BinaryPlaintextMessageContent -> content.valueHex
                else -> null
            }
        }
    }
}
