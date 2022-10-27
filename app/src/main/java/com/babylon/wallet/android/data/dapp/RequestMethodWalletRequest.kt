package com.babylon.wallet.android.data.dapp

data class RequestMethodWalletRequest(
    val method: String,
    val requestId: String,
    val payload: List<RequestMethodWalletPayload>,
    val metadata: RequestMethodMetadata
) {
    data class AccountAddressesRequestMethodWalletRequest(
        val requestType: String,
        val numberOfAddresses: Int?,
        val ongoing: Boolean,
        val reset: Boolean
    ) : RequestMethodWalletPayload

    data class PersonaDataRequestMethodWalletRequest(
        val requestType: String,
        val fields: List<String>,
        val ongoing: Boolean,
        val reset: Boolean,
        val revokeOngoingAccess: List<String>
    ) : RequestMethodWalletPayload

    sealed interface RequestMethodWalletPayload

    data class RequestMethodMetadata(
        val networkId: String,
        val origin: String,
        val dAppId: String
    )
}
