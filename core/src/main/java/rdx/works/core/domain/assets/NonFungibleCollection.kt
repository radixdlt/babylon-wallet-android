package rdx.works.core.domain.assets

import rdx.works.core.domain.resources.Resource

data class NonFungibleCollection(
    val collection: Resource.NonFungibleResource
) : Asset.NonFungible {
    override val resource: Resource.NonFungibleResource
        get() = collection
}
