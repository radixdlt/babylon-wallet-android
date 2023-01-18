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
            name = "adapanet"
        )
        val hammunet = Network(
            id = NetworkId.Hammunet.value,
            name = "hammunet"
        )
        val betanet = Network(
            id = NetworkId.Betanet.value,
            name = "betanet"
        )
        val mardunet = Network(
            id = NetworkId.Mardunet.value,
            name = "mardunet"
        )
        val enkinet = Network(
            id = NetworkId.Enkinet.value,
            name = "enkinet"
        )
        val gilganet = Network(
            id = NetworkId.Gilganet.value,
            name = "gilganet"
        )

        fun allKnownNetworks(): List<Network> {
            return listOf(adapanet, hammunet, betanet, mardunet, enkinet, gilganet)
        }
    }
}
