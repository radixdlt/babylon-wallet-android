package com.babylon.wallet.android.domain.model.messages

sealed interface IncomingRequestResponse {
    data object SuccessRadixMobileConnect : IncomingRequestResponse
    data object SuccessCE : IncomingRequestResponse
}
