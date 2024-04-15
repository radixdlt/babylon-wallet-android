package com.babylon.wallet.android.data.dapp.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
sealed class ConnectorExtensionExchangeInteraction(
    @SerialName("discriminator")
    val discriminator: String
) {

    @Serializable
    data class LinkClient(
        @SerialName("publicKey")
        val publicKey: String
    ) : ConnectorExtensionExchangeInteraction("linkClient")
}