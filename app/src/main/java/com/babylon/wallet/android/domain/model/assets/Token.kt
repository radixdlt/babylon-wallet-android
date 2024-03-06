package com.babylon.wallet.android.domain.model.assets

import com.babylon.wallet.android.domain.model.resources.Resource

data class Token(
    override val resource: Resource.FungibleResource
) : Asset.Fungible
