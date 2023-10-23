package com.babylon.wallet.android.domain.model.assets

import android.net.Uri
import com.babylon.wallet.android.domain.model.resources.Resource

data class StakeClaim(
    val nonFungibleResource: Resource.NonFungibleResource,
) {

    val validatorAddress: String
        get() = nonFungibleResource.validatorAddress.orEmpty()

    val resourceAddress: String
        get() = nonFungibleResource.resourceAddress

    val name: String
        get() = nonFungibleResource.name

    val iconUrl: Uri?
        get() = nonFungibleResource.iconUrl
}
