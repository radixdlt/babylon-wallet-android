package rdx.works.profile.data.model.apppreferences

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import rdx.works.profile.data.model.extensions.containsGateway

@Serializable
data class Gateways(
    @SerialName("current")
    private val currentGatewayUrl: String,

    @SerialName("saved")
    val saved: List<Radix.Gateway>
) {
    fun current(): Radix.Gateway {
        return saved.first { it.url == currentGatewayUrl }
    }

    fun changeCurrent(gateway: Radix.Gateway): Gateways {
        require(saved.containsGateway(gateway))
        return copy(currentGatewayUrl = gateway.url)
    }

    fun add(gateway: Radix.Gateway): Gateways {
        require(saved.all { it.url != gateway.url })
        return copy(saved = saved + gateway)
    }

    fun delete(gateway: Radix.Gateway): Gateways {
        return copy(saved = saved.filter { it.url != gateway.url })
    }

    companion object {

        val preset = Gateways(
            currentGatewayUrl = Radix.Gateway.mainnet.url,
            saved = listOf(Radix.Gateway.mainnet, Radix.Gateway.stokenet)
        )
    }
}
