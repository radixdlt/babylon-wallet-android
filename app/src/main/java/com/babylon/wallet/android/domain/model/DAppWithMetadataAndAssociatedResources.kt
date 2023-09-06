package com.babylon.wallet.android.domain.model

data class DAppWithMetadataAndAssociatedResources(
    val dAppWithMetadata: DAppWithMetadata,
    val resources: DAppResources
) {

    val fungibleResources: List<Resource.FungibleResource>
        get() = resources.fungibleResources

    val nonFungibleResources: List<Resource.NonFungibleResource>
        get() = resources.nonFungibleResources
}

data class DAppResources(
    val fungibleResources: List<Resource.FungibleResource>,
    val nonFungibleResources: List<Resource.NonFungibleResource>
)
