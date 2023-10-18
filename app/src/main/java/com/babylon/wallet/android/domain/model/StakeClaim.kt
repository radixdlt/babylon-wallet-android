package com.babylon.wallet.android.domain.model

import android.net.Uri

data class StakeClaim(
    val nonFungibleResource: Resource.NonFungibleResource,
    val validator: ValidatorWithStakeResources? = null
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
