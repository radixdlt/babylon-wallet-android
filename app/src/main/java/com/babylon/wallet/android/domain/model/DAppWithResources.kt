package com.babylon.wallet.android.domain.model

import com.babylon.wallet.android.domain.model.resources.Resource

data class DAppWithResources(
    val dApp: DApp,
    val fungibleResources: List<Resource.FungibleResource> = emptyList(),
    val nonFungibleResources: List<Resource.NonFungibleResource> = emptyList()
)
