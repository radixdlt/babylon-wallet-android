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
            return NetworkId.values().find { it.value == id } ?: throw IllegalArgumentException("Network ID not valid")
        }

        companion object {
            val hammunet = Network(
                id = NetworkId.Hammunet.value,
                name = "hammunet",
                displayDescription = "Hammunet (Test Network)"
            )
            val nebunet = Network(
                id = NetworkId.Nebunet.value,
                name = "nebunet",
                displayDescription = "Radix Public Network"
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

            fun allKnownNetworks(): List<Network> {
                return listOf(hammunet, nebunet, mardunet, enkinet)
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
            get() = url == nebunet.url

        fun displayName(): String {
            return if (network.id == Network.nebunet.networkId().value) {
                "Radix Betanet Gateway"
            } else {
                url
            }
        }

        fun displayDescription(): String {
            return network.displayDescription
        }

        companion object {
            val default: Gateway
                get() = nebunet

            val nebunet = Gateway(
                url = "https://betanet.radixdlt.com",
                network = Network.nebunet
            )
            val hammunet = Gateway(
                url = "https://hammunet-gateway.radixdlt.com",
                network = Network.hammunet
            )
            val enkinet = Gateway(
                url = "https://enkinet-gateway.radixdlt.com",
                network = Network.enkinet
            )
            val mardunet = Gateway(
                url = "https://mardunet-gateway.radixdlt.com",
                network = Network.mardunet
            )
        }
    }
}
