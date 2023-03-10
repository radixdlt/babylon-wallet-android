package rdx.works.profile.data.model.apppreferences

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.time.Instant

@Serializable
data class AppPreferences(
    @SerialName("display")
    val display: Display,

    @SerialName("gateways")
    val gateways: Gateways,

    @SerialName("p2pClients")
    val p2pClients: List<P2PClient>
)

@Serializable
data class Gateways(

    @SerialName("current")
    private val currentGatewayUrl: String,

    @SerialName("saved")
    val saved: List<Gateway>
) {
    fun current(): Gateway {
        return saved.first { it.url == currentGatewayUrl }
    }

    fun changeCurrent(gateway: Gateway): Gateways {
        require(saved.contains(gateway))
        return copy(currentGatewayUrl = gateway.url)
    }

    fun add(gateway: Gateway): Gateways {
        require(saved.all { it.url != gateway.url })
        return copy(saved = saved + gateway)
    }

    fun delete(gateway: Gateway): Gateways {
        return copy(saved = saved.filter { it.url != gateway.url })
    }
}

@Serializable
data class Gateway(

    @SerialName("url")
    val url: String,

    @SerialName("network")
    val network: Network
) {

    val isDefault: Boolean
        get() = url == nebunet.url

    companion object {
        val hammunet = Gateway(
            url = "https://hammunet-gateway.radixdlt.com",
            network = Network.hammunet
        )
        val nebunet = Gateway(
            url = "https://betanet.radixdlt.com",
            network = Network.nebunet
        )
    }
}

@Serializable
data class P2PClient(
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
        ): P2PClient = P2PClient(
            connectionPassword = connectionPassword,
            displayName = displayName
        )
    }
}
