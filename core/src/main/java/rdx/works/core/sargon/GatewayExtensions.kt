package rdx.works.core.sargon

import com.radixdlt.sargon.Gateway
import com.radixdlt.sargon.extensions.isWellKnown
import com.radixdlt.sargon.extensions.mainnet

val Gateway.Companion.default: Gateway
    get() = Gateway.mainnet

val Gateway.Companion.comparator: Comparator<Gateway>
    get() = object : Comparator<Gateway> {
        override fun compare(thisGateway: Gateway?, otherGateway: Gateway?): Int {
            if (thisGateway == null && otherGateway == null) return 0
            if (thisGateway != null && otherGateway == null) return -1
            if (thisGateway == null && otherGateway != null) return 1

            requireNotNull(thisGateway)
            requireNotNull(otherGateway)

            if (thisGateway.isWellKnown && !otherGateway.isWellKnown) return -1
            if (!thisGateway.isWellKnown && otherGateway.isWellKnown) return 1

            val idDifference = thisGateway.network.id.value - otherGateway.network.id.value
            return if (idDifference == 0u) {
                thisGateway.network.displayDescription.compareTo(otherGateway.network.displayDescription)
            } else {
                idDifference.toInt()
            }
        }

    }