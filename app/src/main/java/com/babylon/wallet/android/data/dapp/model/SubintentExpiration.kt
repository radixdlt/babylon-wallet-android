package com.babylon.wallet.android.data.dapp.model

import com.radixdlt.sargon.DappToWalletInteractionSubintentExpiration
import com.radixdlt.sargon.DappToWalletInteractionSubintentExpireAfterDelay
import com.radixdlt.sargon.DappToWalletInteractionSubintentExpireAtTime
import com.radixdlt.sargon.Timestamp
import kotlin.time.Duration

sealed interface SubintentExpiration {
    data class AtTime(val timestamp: Timestamp) : SubintentExpiration

    data class DelayAfterSign(val delay: Duration) : SubintentExpiration

    fun toDAppInteraction(): DappToWalletInteractionSubintentExpiration = when (this) {
        is AtTime -> DappToWalletInteractionSubintentExpiration.AtTime(
            v1 = DappToWalletInteractionSubintentExpireAtTime(unixTimestampSeconds = timestamp)
        )
        is DelayAfterSign -> DappToWalletInteractionSubintentExpiration.AfterDelay(
            v1 = DappToWalletInteractionSubintentExpireAfterDelay(expireAfterSeconds = delay.inWholeSeconds.toULong())
        )
    }
}
