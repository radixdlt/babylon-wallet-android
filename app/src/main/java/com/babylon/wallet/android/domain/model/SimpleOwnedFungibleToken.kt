package com.babylon.wallet.android.domain.model

import java.math.BigDecimal

data class SimpleOwnedFungibleToken(
    val owner: AccountAddress,
    val amount: BigDecimal,
    val address: String,
)
