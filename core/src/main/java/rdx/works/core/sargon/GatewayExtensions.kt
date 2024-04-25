package rdx.works.core.sargon

import com.radixdlt.sargon.Gateway
import com.radixdlt.sargon.extensions.mainnet

val Gateway.Companion.default: Gateway
    get() = Gateway.mainnet
