package com.babylon.wallet.android.domain.model

import java.math.BigDecimal

data class SimpleOwnedNonFungibleToken(
    val owner: AccountAddress,
    val amount: BigDecimal,
    val tokenResourceAddress: String
)
