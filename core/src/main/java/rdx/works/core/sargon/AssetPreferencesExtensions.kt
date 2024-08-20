package rdx.works.core.sargon

import com.radixdlt.sargon.AssetAddress
import com.radixdlt.sargon.AssetPreference
import com.radixdlt.sargon.NonFungibleGlobalId
import com.radixdlt.sargon.PoolAddress
import com.radixdlt.sargon.ResourceAddress
import com.radixdlt.sargon.extensions.asIdentifiable
import com.radixdlt.sargon.extensions.hiddenAssets

fun List<AssetPreference>.hidden(): List<AssetAddress> = asIdentifiable().hiddenAssets

fun List<AssetPreference>.hiddenFungibles(): List<ResourceAddress> = hidden().fungibles()

fun List<AssetPreference>.hiddenNonFungibles(): List<NonFungibleGlobalId> = hidden().nonFungibles()

fun List<AssetPreference>.hiddenPools(): List<PoolAddress> = hidden().pools()

fun List<AssetAddress>.fungibles(): List<ResourceAddress> = mapNotNull { (it as? AssetAddress.Fungible)?.v1 }

fun List<AssetAddress>.nonFungibles(): List<NonFungibleGlobalId> = mapNotNull { (it as? AssetAddress.NonFungible)?.v1 }

fun List<AssetAddress>.pools(): List<PoolAddress> = mapNotNull { (it as? AssetAddress.PoolUnit)?.v1 }