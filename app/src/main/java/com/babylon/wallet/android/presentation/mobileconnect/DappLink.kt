package com.babylon.wallet.android.presentation.mobileconnect

import com.radixdlt.sargon.AccountAddress
import com.radixdlt.sargon.extensions.init
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class DappLink(
    @SerialName("origin")
    val origin: String,
    @SerialName("dAppDefinitionAddress")
    private val address: String,
    @SerialName("secret")
    val secret: String,
    @SerialName("sessionId")
    val sessionId: String,
    @SerialName("privateKey")
    val x25519PrivateKeyCompressed: String,
    @SerialName("callbackPath")
    val callbackPath: String? = null
) {

    val dAppDefinitionAddress
        get() = AccountAddress.init(address)
}
