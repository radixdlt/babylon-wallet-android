package rdx.works.profile.data.model.apppreferences

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import rdx.works.profile.derivation.model.NetworkId

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
