package com.babylon.wallet.android.domain.model.resources

data class Pool(
    val address: String,
    val resources: List<Resource.FungibleResource>
)
