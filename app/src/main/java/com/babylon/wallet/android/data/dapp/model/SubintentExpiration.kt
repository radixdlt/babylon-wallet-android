package com.babylon.wallet.android.data.dapp.model

import com.radixdlt.sargon.Timestamp
import kotlin.time.Duration

sealed interface SubintentExpiration {
    data class AtTime(val timestamp: Timestamp) : SubintentExpiration

    data class DelayAfterSign(val delay: Duration) : SubintentExpiration

    data object None : SubintentExpiration
}
