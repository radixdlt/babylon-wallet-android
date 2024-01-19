package com.babylon.wallet.android.domain.model.assets

import com.babylon.wallet.android.domain.model.resources.Resource

data class NonFungibleCollection(
    val collection: Resource.NonFungibleResource
) : Asset {
    override val resource: Resource
        get() = collection
}
