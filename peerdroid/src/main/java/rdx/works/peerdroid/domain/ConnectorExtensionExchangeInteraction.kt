package rdx.works.peerdroid.domain

import com.radixdlt.sargon.PublicKey
import com.radixdlt.sargon.Signature
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import rdx.works.core.serializers.PublicKeySerializer
import rdx.works.core.serializers.SignatureSerializer

@Serializable
sealed class ConnectorExtensionExchangeInteraction(
    @SerialName("discriminator")
    val discriminator: String
) {

    @Serializable
    data class LinkClient(
        @Serializable(with = PublicKeySerializer::class)
        @SerialName("publicKey")
        val publicKey: PublicKey,
        @Serializable(with = SignatureSerializer::class)
        @SerialName("signature")
        val signature: Signature
    ) : ConnectorExtensionExchangeInteraction("linkClient")
}
