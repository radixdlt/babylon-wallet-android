package com.babylon.wallet.android.domain.model.assets

import rdx.works.core.domain.resources.Resource

data class Token(
    override val resource: Resource.FungibleResource
) : Asset.Fungible
