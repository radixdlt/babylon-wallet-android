package com.babylon.wallet.android.presentation.mobileconnect

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import rdx.works.core.HexCoded32Bytes
import rdx.works.core.Identified

@Serializable
data class DappLink(
    @SerialName("origin")
    val origin: String,
    @SerialName("dAppDefinitionAddress")
    val dAppDefinitionAddress: String,
    @SerialName("secret")
    val secret: HexCoded32Bytes,
    @SerialName("sessionId")
    val sessionId: String,
    @SerialName("privateKey")
    val x25519PrivateKeyCompressed: HexCoded32Bytes,
    @SerialName("callbackPath")
    val callbackPath: String? = null
) : Identified {
    override val identifier: String
        get() = dAppDefinitionAddress
}
