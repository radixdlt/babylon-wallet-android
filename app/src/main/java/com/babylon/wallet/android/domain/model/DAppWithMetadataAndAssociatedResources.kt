package com.babylon.wallet.android.domain.model

import com.babylon.wallet.android.domain.model.resources.Resource

data class DAppWithMetadataAndAssociatedResources(
    val dAppWithMetadata: DAppWithMetadata,
    val resources: DAppResources,
    val verified: Boolean = true
) {

    val fungibleResources: List<Resource.FungibleResource>
        get() = resources.fungibleResources

    val nonFungibleResources: List<Resource.NonFungibleResource>
        get() = resources.nonFungibleResources
}

data class DAppResources(
    val fungibleResources: List<Resource.FungibleResource> = emptyList(),
    val nonFungibleResources: List<Resource.NonFungibleResource> = emptyList()
)
