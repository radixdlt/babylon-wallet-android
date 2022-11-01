package com.babylon.wallet.android.data.dapp.model

data class RequestMethodWalletRequest(
    val method: String,
    val requestId: String,
    val payload: List<AccountAddressesRequestMethodWalletRequest>,
    val metadata: RequestMethodMetadata
) {
    data class AccountAddressesRequestMethodWalletRequest(
        val requestType: String,
        val numberOfAddresses: Int?
    )

    data class RequestMethodMetadata(
        val networkId: String,
        val origin: String,
        val dAppId: String
    )
}
