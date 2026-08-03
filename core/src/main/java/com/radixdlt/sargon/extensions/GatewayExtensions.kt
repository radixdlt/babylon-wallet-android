package com.radixdlt.sargon.extensions

import com.radixdlt.sargon.Decimal192
import com.radixdlt.sargon.Gateway
import com.radixdlt.sargon.NetworkId
import com.radixdlt.sargon.TransactionManifest

fun Gateway.Companion.forNetwork(networkId: NetworkId): Gateway = when (networkId) {
    NetworkId.MAINNET -> Gateway.mainnet
    NetworkId.STOKENET -> Gateway.stokenet
    else -> Gateway.init(url = "https://custom-${networkId.string}.radixdlt.com", networkId = networkId)
}

fun Decimal192.Companion.init(formattedString: String): Decimal192 =
    formattedString.toDecimal192()

val TransactionManifest.instructions: String
    get() = instructionsString
