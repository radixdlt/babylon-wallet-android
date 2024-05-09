package rdx.works.profile.data.model.apppreferences

import com.radixdlt.sargon.PublicKey
import com.radixdlt.sargon.RadixConnectPassword
import com.radixdlt.sargon.extensions.hex
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import rdx.works.core.serializers.PublicKeySerializer
import rdx.works.core.serializers.RadixConnectPasswordSerializer

@Serializable
data class P2PLink(
    /**
     * The most important property of this struct, this password,
     * is used to be able to reestablish the P2P connection.
     */
    @Serializable(with = RadixConnectPasswordSerializer::class)
    @SerialName("connectionPassword")
    val connectionPassword: RadixConnectPassword,

    /**
     * Client name, e.g. "Chrome on Macbook" or "My work Android"
     */
    @SerialName("displayName")
    val displayName: String,

    @Serializable(with = PublicKeySerializer::class)
    @SerialName("publicKey")
    val publicKey: PublicKey,

    @SerialName("purpose")
    val purpose: P2PLinkPurpose
) {

    val id
        get() = publicKey.hex
}
