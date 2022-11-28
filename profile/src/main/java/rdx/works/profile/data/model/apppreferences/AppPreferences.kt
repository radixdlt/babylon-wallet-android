package rdx.works.profile.data.model.apppreferences

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
    val p2pClients: List<P2PClient>
)

@Serializable
data class NetworkAndGateway(

    @SerialName("gatewayAPIEndpointURL")
    val gatewayAPIEndpointURL: String,

    @SerialName("network")
    val network: Network
) {

    companion object {
        val hammunet = NetworkAndGateway(
            gatewayAPIEndpointURL = "https://hammunet-gateway.radixdlt.com",
            network = Network.hammunet
        )
    }
}

@Serializable
data class P2PClient(
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
        ): P2PClient {
            val now = Date().toString()
            return P2PClient(
                connectionPassword = connectionPassword,
                displayName = displayName,
                firstEstablishedOn = now,
                lastUsedOn = now,
            )
        }
    }
}
