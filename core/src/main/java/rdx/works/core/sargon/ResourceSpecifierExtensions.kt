package rdx.works.core.sargon

import com.radixdlt.sargon.NonFungibleGlobalId
import com.radixdlt.sargon.ResourceOrNonFungible
import com.radixdlt.sargon.ResourceSpecifier

fun ResourceSpecifier.toResourceOrNonFungible(): List<ResourceOrNonFungible> = when (this) {
    is ResourceSpecifier.Fungible -> listOf(ResourceOrNonFungible.Resource(resourceAddress))
    is ResourceSpecifier.NonFungible -> ids.map { localId ->
        ResourceOrNonFungible.NonFungible(
            NonFungibleGlobalId(
                resourceAddress = resourceAddress,
                nonFungibleLocalId = localId
            )
        )
    }
}
