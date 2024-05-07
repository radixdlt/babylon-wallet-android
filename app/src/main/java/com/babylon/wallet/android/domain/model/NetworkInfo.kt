package com.babylon.wallet.android.domain.model

import com.radixdlt.sargon.NetworkId

data class NetworkInfo(
    val id: NetworkId,
    val epoch: Long
)
