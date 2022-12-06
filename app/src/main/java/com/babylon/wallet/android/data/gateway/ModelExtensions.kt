package com.babylon.wallet.android.data.gateway

import com.babylon.wallet.android.data.gateway.generated.model.ErrorResponse

fun ErrorResponse.hasMessageOrDetails(): Boolean {
    return message != null && details != null
}
