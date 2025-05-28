package com.babylon.wallet.android.presentation.transfer.accounts

import com.radixdlt.sargon.AccountAddress

data class RnsDomain(
    val accountAddress: AccountAddress,
    val imageUrl: String,
    val name: String
)