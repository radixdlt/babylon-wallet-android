package rdx.works.core.sargon

import com.radixdlt.sargon.Gateway
import com.radixdlt.sargon.extensions.isWellKnown
import com.radixdlt.sargon.extensions.mainnet

val Gateway.Companion.default: Gateway
    get() = Gateway.mainnet

@Suppress("ReturnCount")
val Gateway.Companion.comparator: Comparator<Gateway>
    get() = Comparator { thisGateway, otherGateway ->
        if (thisGateway == null && otherGateway == null) return@Comparator 0
        if (thisGateway != null && otherGateway == null) return@Comparator -1
        if (thisGateway == null && otherGateway != null) return@Comparator 1

        requireNotNull(thisGateway)
        requireNotNull(otherGateway)

        if (thisGateway.isWellKnown && !otherGateway.isWellKnown) return@Comparator -1
        if (!thisGateway.isWellKnown && otherGateway.isWellKnown) return@Comparator 1

        val idDifference = thisGateway.network.id.value - otherGateway.network.id.value
        if (idDifference == 0u) {
            thisGateway.network.displayDescription.compareTo(otherGateway.network.displayDescription)
        } else {
            idDifference.toInt()
        }
    }
