package rdx.works.peerdroid.domain

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
        val publicKey: String,
        @SerialName("signature")
        val signature: String
    ) : ConnectorExtensionExchangeInteraction("linkClient")
}