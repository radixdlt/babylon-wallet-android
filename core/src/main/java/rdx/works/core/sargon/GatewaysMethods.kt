package rdx.works.core.sargon

import com.radixdlt.sargon.Gateway
import com.radixdlt.sargon.Gateways
import java.net.URI

fun Gateways.changeCurrent(gateway: Gateway): Gateways {
    require(other.containsGateway(gateway))
    return copy(current = gateway)
}

fun Gateways.add(gateway: Gateway): Gateways {
    require(other.all { it.url.toString() != gateway.url.toString() })
    return copy(other = other + gateway)
}

fun Gateways.delete(gateway: Gateway): Gateways {
    return copy(other = other.filter { it.url.toString() != gateway.url.toString() })
}

private fun List<Gateway>.containsGateway(gateway: Gateway): Boolean {
    return this.any {
        val existingGatewayUri = URI.create(it.url.toString().removeSuffix("/"))
        val gatewayUri = URI.create(gateway.url.toString().removeSuffix("/"))

        it.network.id == gateway.network.id && gatewayUri.equals(existingGatewayUri)
    }
}