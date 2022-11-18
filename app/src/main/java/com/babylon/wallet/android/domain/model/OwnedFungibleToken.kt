package com.babylon.wallet.android.domain.model

import java.math.BigDecimal

data class OwnedFungibleToken(
    val owner: AccountAddress,
    val amount: BigDecimal,
    val address: String,
    val token: FungibleToken
)