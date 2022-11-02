package com.babylon.wallet.android.data.dapp.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class RequestMethodWalletRequest(
    @SerialName("method")
    val method: String,
    @SerialName("requestId")
    val requestId: String,
    @SerialName("payload")
    val payload: List<AccountAddressesRequestMethodWalletRequest>,
    @SerialName("metadata")
    val metadata: RequestMethodMetadata
) {
    // TODO Remember to add PersonaDataRequestMethodWalletRequest when needed
    //  {
    //       "requestType": "personaData",
    //       "fields": ["firstName", "email"]
    //    }
    @Serializable
    data class AccountAddressesRequestMethodWalletRequest(
        @SerialName("requestType")
        val requestType: String,
        @SerialName("numberOfAddresses")
        val numberOfAddresses: Int?
    )

    @Serializable
    data class RequestMethodMetadata(
        @SerialName("networkId")
        val networkId: String,
        @SerialName("origin")
        val origin: String,
        @SerialName("dAppId")
        val dAppId: String
    )

    enum class RequestType(val value: String) {
        ACCOUNT_ADDRESSES("accountAddresses")
    }
}
