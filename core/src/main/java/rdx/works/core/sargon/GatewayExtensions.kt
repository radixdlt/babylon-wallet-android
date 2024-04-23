package rdx.works.core.sargon

import com.radixdlt.sargon.Gateway
import com.radixdlt.sargon.NetworkId
import com.radixdlt.sargon.extensions.mainnet
import com.radixdlt.sargon.gatewayIsWellknown
import com.radixdlt.sargon.gatewayToString
import com.radixdlt.sargon.newGatewayForNetworkId

val Gateway.Companion.default: Gateway
    get() = Gateway.mainnet



