package rdx.works.profile.model.apppreferences

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
    }
}
