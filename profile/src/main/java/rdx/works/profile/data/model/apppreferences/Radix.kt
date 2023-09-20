package rdx.works.profile.data.model.apppreferences

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import rdx.works.profile.derivation.model.NetworkId

object Radix {

    @Serializable
    data class Network(
        @SerialName("id")
        val id: Int,

        @SerialName("name")
        val name: String,

        @SerialName("displayDescription")
        val displayDescription: String
    ) {

        fun networkId(): NetworkId {
            return NetworkId.values().find { it.value == id }
                ?: throw IllegalArgumentException("Network ID not valid")
        }

        companion object {
            val mainnet = Network(
                id = NetworkId.Mainnet.value,
                name = "mainnet",
                displayDescription = "Mainnet"
            )
            val stokenet = Network(
                id = NetworkId.Stokenet.value,
                name = "stokenet",
                displayDescription = "Stokenet"
            )
            val hammunet = Network(
                id = NetworkId.Hammunet.value,
                name = "hammunet",
                displayDescription = "Hammunet (Test Network)"
            )
            val nebunet = Network(
                id = NetworkId.Nebunet.value,
                name = "nebunet",
                displayDescription = "Betanet"
            )
            val kisharnet = Network(
                id = NetworkId.Kisharnet.value,
                name = "kisharnet",
                displayDescription = "RCnet"
            )
            val mardunet = Network(
                id = NetworkId.Mardunet.value,
                name = "mardunet",
                displayDescription = "Mardunet (Test Network)"
            )
            val enkinet = Network(
                id = NetworkId.Enkinet.value,
                name = "enkinet",
                displayDescription = "Enkinet (Test Network)"
            )
            val ansharnet = Network(
                id = NetworkId.Ansharnet.value,
                name = "ansharnet",
                displayDescription = "RCnet-V2 test network"
            )
            val zabanet = Network(
                id = NetworkId.Zabanet.value,
                name = "zabanet",
                displayDescription = "RCnet-V3 test network"
            )

            fun allKnownNetworks(): List<Network> {
                return listOf(mainnet, stokenet, hammunet, nebunet, kisharnet, mardunet, enkinet, ansharnet, zabanet)
            }

            fun forName(name: String): Network {
                return allKnownNetworks().first { network ->
                    network.name == name
                }
            }
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
            get() = url == default.url

        fun displayDescription(): String {
            return network.displayDescription
        }

        companion object {
            val nebunet = Gateway(
                url = "https://betanet.radixdlt.com/",
                network = Network.nebunet
            )
            val kisharnet = Gateway(
                url = "https://rcnet.radixdlt.com/",
                network = Network.kisharnet
            )
            val hammunet = Gateway(
                url = "https://hammunet-gateway.radixdlt.com/",
                network = Network.hammunet
            )
            val enkinet = Gateway(
                url = "https://enkinet-gateway.radixdlt.com/",
                network = Network.enkinet
            )
            val mardunet = Gateway(
                url = "https://mardunet-gateway.radixdlt.com/",
                network = Network.mardunet
            )
            val ansharnet = Gateway(
                url = "https://ansharnet-gateway.radixdlt.com/",
                network = Network.ansharnet
            )
            val rcnetV3 = Gateway(
                url = "https://rcnet-v3.radixdlt.com/",
                network = Network.zabanet
            )
            val stokenet = Gateway(
                url = "https://babylon-stokenet-gateway.radixdlt.com/",
                network = Network.stokenet
            )
            val mainnet = Gateway(
                url = "https://mainnet.radixdlt.com/",
                network = Network.mainnet
            )

            var default: Gateway = rcnetV3
                // TODO To remove when mainnet becomes default and change to val
                set(value) {
                    // Can change default Gateway, only if the
                    // value is Mainnet
                    if (value == mainnet) {
                        field = mainnet
                    }
                }
        }
    }

    fun NetworkId.dashboardUrl() =
        when (this) {
            NetworkId.Mainnet -> {
                DASHBOARD_MAINNET_URL
            }

            NetworkId.Zabanet -> {
                DASHBOARD_RCNET_V3_URL
            }

            NetworkId.Stokenet -> {
                DASHBOARD_STOKENET_URL
            }

            NetworkId.Hammunet -> {
                DASHBOARD_HAMMUNET_URL
            }

            NetworkId.Enkinet -> {
                DASHBOARD_ENKINET_URL
            }

            NetworkId.Mardunet -> {
                DASHBOARD_MARDUNET_URL
            }

            NetworkId.Kisharnet -> {
                DASHBOARD_KISHARNET_URL
            }

            else -> {
                DASHBOARD_MAINNET_URL
            }
        }

    private const val DASHBOARD_MAINNET_URL = "https://dashboard.radixdlt.com"
    private const val DASHBOARD_RCNET_V3_URL = "https://rcnet-v3-dashboard.radixdlt.com"
    private const val DASHBOARD_STOKENET_URL = "https://stokenet-dashboard.radixdlt.com"
    private const val DASHBOARD_HAMMUNET_URL = "https://hammunet-dashboard.rdx-works-main.extratools.works"
    private const val DASHBOARD_ENKINET_URL = "https://enkinet-dashboard.rdx-works-main.extratools.works"
    private const val DASHBOARD_MARDUNET_URL = "https://mardunet-dashboard.rdx-works-main.extratools.works"
    private const val DASHBOARD_KISHARNET_URL = "https://kisharnet-dashboard.radixdlt.com"
}
