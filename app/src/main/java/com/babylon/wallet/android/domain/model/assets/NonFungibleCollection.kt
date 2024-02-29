package com.babylon.wallet.android.domain.model.assets

import rdx.works.core.domain.resources.Resource

data class NonFungibleCollection(
    val collection: Resource.NonFungibleResource
) : Asset.NonFungible {
    override val resource: Resource.NonFungibleResource
        get() = collection
}
