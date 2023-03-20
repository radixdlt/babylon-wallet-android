package rdx.works.profile.data.model.apppreferences

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import rdx.works.profile.data.model.Profile

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

    companion object {

        fun fromCurrent(current: Gateway) = Gateways(
            currentGatewayUrl = current.url,
            saved = listOf(current)
        )
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

fun Profile.changeGateway(
    gateway: Gateway
): Profile {
    val gateways = appPreferences.gateways.changeCurrent(gateway)
    val appPreferences = appPreferences.copy(gateways = gateways)
    return copy(appPreferences = appPreferences)
}

fun Profile.addGateway(
    gateway: Gateway
): Profile {
    val updatedGateways = appPreferences.gateways.add(gateway)
    return copy(appPreferences = appPreferences.copy(gateways = updatedGateways))
}

fun Profile.deleteGateway(
    gateway: Gateway
): Profile {
    val updatedGateways = appPreferences.gateways.delete(gateway)
    return copy(appPreferences = appPreferences.copy(gateways = updatedGateways))
}
