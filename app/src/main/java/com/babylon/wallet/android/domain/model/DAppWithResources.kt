package com.babylon.wallet.android.domain.model

import com.babylon.wallet.android.domain.model.resources.Resource

data class DAppWithResources(
    val dApp: DApp,
    val resources: DAppResources,
    val verified: Boolean = true,
    val componentAddresses: List<String> = emptyList()
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
