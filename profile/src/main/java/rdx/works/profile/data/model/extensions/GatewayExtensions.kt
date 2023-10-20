package rdx.works.profile.data.model.extensions

import rdx.works.profile.data.model.Profile
import rdx.works.profile.data.model.apppreferences.Radix
import java.net.URI

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
        val existingGatewayUri = URI.create(it.url.removeSuffix("/"))
        val gatewayUri = URI.create(gateway.url.removeSuffix("/"))

        it.network.id == gateway.network.id && gatewayUri.equals(existingGatewayUri)
    }
}
