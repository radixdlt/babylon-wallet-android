package com.babylon.wallet.android.domain.model

data class OwnedNonFungibleToken(
    val owner: AccountAddress,
    val amount: Long,
    val tokenResourceAddress: String,
    val token: NonFungibleToken? = null
)
