package rdx.works.profile.model.apppreferences

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.util.Date

@Serializable
data class AppPreferences(
    @SerialName("display")
    val display: Display,

    @SerialName("networkAndGateway")
    val networkAndGateway: NetworkAndGateway,

    @SerialName("p2pClients")
    val p2pClients: P2PClients
)

@Serializable
data class NetworkAndGateway(

    @SerialName("gatewayAPIEndpointURL")
    val gatewayAPIEndpointURL: String,

    @SerialName("network")
    val network: Network
) {

    companion object {
        val primary = NetworkAndGateway(
            gatewayAPIEndpointURL = "https://alphanet.radixdlt.com/v0",
            network = Network.adapanet
        )
    }
}

@Serializable
data class P2PClients(
    @SerialName("connections")
    val connections: List<Connection>
)

@Serializable
data class Connection(
    @SerialName("connectionPassword")
    val connectionPassword: String,

    /**
     * Client name, e.g. "Chrome on Macbook" or "My work Android"
     */
    @SerialName("displayName")
    val displayName: String,

    @SerialName("firstEstablishedOn")
    val firstEstablishedOn: String,

    @SerialName("lastUsedOn")
    val lastUsedOn: String
) {
    companion object {
        fun init(
            connectionPassword: String,
            displayName: String
        ): Connection {
            return Connection(
                connectionPassword = connectionPassword,
                displayName = displayName,
                firstEstablishedOn = Date().toString(),
                lastUsedOn = Date().toString(),
            )
        }
    }
}
