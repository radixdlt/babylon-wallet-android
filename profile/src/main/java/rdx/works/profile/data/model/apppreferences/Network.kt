package rdx.works.profile.data.model.apppreferences

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import rdx.works.profile.derivation.model.NetworkId

@Serializable
data class Network(
    @SerialName("id")
    val id: Int,

    @SerialName("name")
    val name: String
) {

    fun networkId(): NetworkId {
        return NetworkId.values().find { it.value == id } ?: throw IllegalArgumentException("Network ID not valid")
    }

    companion object {
        val adapanet = Network(
            id = NetworkId.Adapanet.value,
            name = "Adapanet"
        )
        val hammunet = Network(
            id = NetworkId.Hammunet.value,
            name = "Hammunet"
        )
        val nebunet = Network(
            id = NetworkId.Nebunet.value,
            name = "Nebunet"
        )
        val mardunet = Network(
            id = NetworkId.Mardunet.value,
            name = "Mardunet"
        )
        val enkinet = Network(
            id = NetworkId.Enkinet.value,
            name = "Enkinet"
        )
        val gilganet = Network(
            id = NetworkId.Gilganet.value,
            name = "Gilganet"
        )

        fun allKnownNetworks(): List<Network> {
            return listOf(adapanet, hammunet, nebunet, mardunet, enkinet, gilganet)
        }
    }
}
