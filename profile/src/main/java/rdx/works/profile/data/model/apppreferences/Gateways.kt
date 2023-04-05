package rdx.works.profile.data.model.apppreferences

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import rdx.works.profile.data.model.Profile

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
        require(saved.contains(gateway))
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

        fun fromCurrent(current: Radix.Gateway) = Gateways(
            currentGatewayUrl = current.url,
            saved = listOf(current)
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
