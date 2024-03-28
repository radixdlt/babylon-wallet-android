package com.babylon.wallet.android.domain.model

import rdx.works.core.domain.DApp
import rdx.works.core.domain.resources.Resource

data class DAppWithResources(
    val dApp: DApp,
    val fungibleResources: List<Resource.FungibleResource> = emptyList(),
    val nonFungibleResources: List<Resource.NonFungibleResource> = emptyList()
)
