package com.babylon.wallet.android.domain.model

data class NonFungibleTokenId(
    val idHex: String?,
    val immutableDataHex: String? = null,
    val mutableDataHex: String? = null
)
