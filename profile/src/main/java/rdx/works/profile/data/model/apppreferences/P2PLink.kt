package rdx.works.profile.data.model.apppreferences

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class P2PLink(
    /**
     * The most important property of this struct, this password,
     * is used to be able to reestablish the P2P connection.
     */
    @SerialName("connectionPassword")
    val connectionPassword: String,

    /**
     * Client name, e.g. "Chrome on Macbook" or "My work Android"
     */
    @SerialName("displayName")
    val displayName: String,

    @SerialName("publicKey")
    val publicKey: String,

    @SerialName("purpose")
    val purpose: P2PLinkPurpose,

    @SerialName("walletPrivateKey")
    val walletPrivateKey: String
)