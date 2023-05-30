package com.babylon.wallet.android.domain.model

data class DAppWithAssociatedResources(
    val dAppWithMetadata: DAppWithMetadata,
    val resources: Resources?
) {

    val fungibleResources: List<Resource.FungibleResource>
        get() = resources?.fungibleResources.orEmpty()

    val nonFungibleResources: List<Resource.NonFungibleResource>
        get() = resources?.nonFungibleResources.orEmpty()
}
