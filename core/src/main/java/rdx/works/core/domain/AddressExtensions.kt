package rdx.works.core.domain

import com.radixdlt.sargon.AccountAddress
import com.radixdlt.sargon.NetworkId
import com.radixdlt.sargon.NonFungibleGlobalId
import com.radixdlt.sargon.ResourceAddress
import com.radixdlt.sargon.extensions.init
import com.radixdlt.sargon.extensions.networkId

fun AccountAddress.Companion.validatedOnNetworkOrNull(validating: String, networkId: NetworkId) = runCatching {
    AccountAddress.init(validating)
}.getOrNull()?.takeIf { it.networkId == networkId }

fun ResourceAddress.Companion.validatedOnNetworkOrNull(validating: String, networkId: NetworkId) = runCatching {
    ResourceAddress.init(validating)
}.getOrNull()?.takeIf { it.networkId == networkId }

fun NonFungibleGlobalId.Companion.validatedOnNetworkOrNull(validating: String, networkId: NetworkId) = runCatching {
    NonFungibleGlobalId.init(validating)
}.getOrNull()?.takeIf { it.resourceAddress.networkId == networkId }