package com.babylon.wallet.android.domain.model

sealed interface IncomingRequest {

    data class AccountsRequest(
        val requestId: String,
        val isOngoing: Boolean,
        val requiresProofOfOwnership: Boolean,
        val numberOfAccounts: Int
    ) : IncomingRequest

    object SomeOtherRequest : IncomingRequest

    object Empty : IncomingRequest
}
