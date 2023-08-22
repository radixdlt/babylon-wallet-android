package rdx.works.profile.data.model.apppreferences

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import rdx.works.profile.data.model.Profile

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

fun Profile.addP2PLink(
    p2pLink: P2PLink
): Profile {
    val updatedP2PLinks = appPreferences.p2pLinks.toMutableList()
    updatedP2PLinks.add(
        p2pLink
    )

    val newAppPreferences = AppPreferences(
        transaction = appPreferences.transaction,
        display = appPreferences.display,
        security = appPreferences.security,
        gateways = appPreferences.gateways,
        p2pLinks = updatedP2PLinks.toList()
    )

    return this.copy(
        appPreferences = newAppPreferences,
        networks = networks,
    )
}

fun Profile.deleteP2PLink(connectionPassword: String): Profile {
    val updatedP2PLinks = appPreferences.p2pLinks.toMutableList()
    updatedP2PLinks.removeIf { p2pLink ->
        p2pLink.connectionPassword == connectionPassword
    }

    val newAppPreferences = AppPreferences(
        transaction = appPreferences.transaction,
        display = appPreferences.display,
        security = appPreferences.security,
        gateways = appPreferences.gateways,
        p2pLinks = updatedP2PLinks.toList()
    )

    return this.copy(
        appPreferences = newAppPreferences,
        networks = networks,
    )
}
