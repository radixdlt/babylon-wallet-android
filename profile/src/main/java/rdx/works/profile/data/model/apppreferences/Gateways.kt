package rdx.works.profile.data.model.apppreferences

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import rdx.works.profile.data.model.Profile
import java.net.URI

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

fun Profile.changeGateway(
    gateway: Radix.Gateway
): Profile {
    val gateways = appPreferences.gateways.changeCurrent(gateway)
    val appPreferences = appPreferences.copy(gateways = gateways)
    return copy(appPreferences = appPreferences)
}

fun Profile.addGateway(
    gateway: Radix.Gateway
): Profile {
    val updatedGateways = appPreferences.gateways.add(gateway)
    return copy(appPreferences = appPreferences.copy(gateways = updatedGateways))
}

fun Profile.deleteGateway(
    gateway: Radix.Gateway
): Profile {
    val updatedGateways = appPreferences.gateways.delete(gateway)
    return copy(appPreferences = appPreferences.copy(gateways = updatedGateways))
}

fun List<Radix.Gateway>.containsGateway(gateway: Radix.Gateway): Boolean {
    return this.any {
        // example: if the url is "https://mainnet.radixdlt.com" then the host is mainnet.radixdlt.com
        // example: if the url is "https://mainnet.radixdlt.com/" then the host is mainnet.radixdlt.com
        val existingGateway = URI.create(it.url).host
        val gatewayHost = URI.create(gateway.url).host
        it.network.id == gateway.network.id && existingGateway == gatewayHost
    }
}
