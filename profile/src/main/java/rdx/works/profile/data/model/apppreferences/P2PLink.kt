package rdx.works.profile.data.model.apppreferences

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class P2PLink(
    /**
     * The most important property of this struct, this password,
     * is used to be able to reestablish the P2P connection and also acts as the seed
     * for the [id].
     */
    @SerialName("connectionPassword")
    val connectionPassword: String,

    /**
     * Client name, e.g. "Chrome on Macbook" or "My work Android"
     */
    @SerialName("displayName")
    val displayName: String
) {

    val id: String
        get() = connectionPassword

    companion object {
        fun init(
            connectionPassword: String,
            displayName: String
        ): P2PLink = P2PLink(
            connectionPassword = connectionPassword,
            displayName = displayName
        )
    }
}
