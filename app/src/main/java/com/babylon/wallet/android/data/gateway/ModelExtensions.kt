package com.babylon.wallet.android.data.gateway

import com.babylon.wallet.android.data.gateway.generated.model.ErrorResponse

fun ErrorResponse.hasMeaningfulData(): Boolean {
    return message != null && details != null
}
