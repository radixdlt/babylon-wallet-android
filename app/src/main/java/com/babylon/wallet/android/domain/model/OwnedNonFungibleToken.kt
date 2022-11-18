package com.babylon.wallet.android.domain.model

import java.math.BigDecimal

data class OwnedNonFungibleToken(
    val owner: AccountAddress,
    val amount: BigDecimal,
    val tokenResourceAddress: String,
    val token: NonFungibleToken? = null
)
