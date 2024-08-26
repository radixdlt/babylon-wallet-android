package rdx.works.core.sargon

import com.radixdlt.sargon.PoolAddress
import com.radixdlt.sargon.ResourceAddress
import com.radixdlt.sargon.ResourceAppPreference
import com.radixdlt.sargon.ResourceIdentifier
import com.radixdlt.sargon.extensions.asIdentifiable
import com.radixdlt.sargon.extensions.hiddenResources

fun List<ResourceAppPreference>.hidden(): List<ResourceIdentifier> = asIdentifiable().hiddenResources

fun List<ResourceAppPreference>.hiddenFungibles(): List<ResourceAddress> = hidden().fungibles()

fun List<ResourceAppPreference>.hiddenNonFungibles(): List<ResourceAddress> = hidden().nonFungibles()

fun List<ResourceAppPreference>.hiddenPools(): List<PoolAddress> = hidden().pools()

fun List<ResourceIdentifier>.fungibles(): List<ResourceAddress> = mapNotNull { (it as? ResourceIdentifier.Fungible)?.v1 }

fun List<ResourceIdentifier>.nonFungibles(): List<ResourceAddress> = mapNotNull { (it as? ResourceIdentifier.NonFungible)?.v1 }

fun List<ResourceIdentifier>.pools(): List<PoolAddress> = mapNotNull { (it as? ResourceIdentifier.PoolUnit)?.v1 }
