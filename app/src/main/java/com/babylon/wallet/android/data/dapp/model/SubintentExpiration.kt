package com.babylon.wallet.android.data.dapp.model

import com.radixdlt.sargon.DappToWalletInteractionSubintentExpiration
import com.radixdlt.sargon.DappToWalletInteractionSubintentExpireAfterDelay
import com.radixdlt.sargon.DappToWalletInteractionSubintentExpireAtTime
import com.radixdlt.sargon.Timestamp
import java.time.Instant
import java.time.ZoneId
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

sealed interface SubintentExpiration {

    fun toDAppInteraction(): DappToWalletInteractionSubintentExpiration

    fun expirationTimestamp(): Timestamp

    data class AtTime(
        private val expireAt: Duration
    ) : SubintentExpiration {

        override fun toDAppInteraction(): DappToWalletInteractionSubintentExpiration = DappToWalletInteractionSubintentExpiration.AtTime(
            DappToWalletInteractionSubintentExpireAtTime(unixTimestampSeconds = expireAt.inWholeSeconds.toULong())
        )

        override fun expirationTimestamp(): Timestamp =
            Timestamp.ofInstant(Instant.ofEpochMilli(expireAt.inWholeMilliseconds), ZoneId.systemDefault())

        fun expirationDuration(): Duration = (expireAt.inWholeSeconds - Instant.now().epochSecond).coerceAtLeast(0L).seconds
    }

    data class DelayAfterSign(val delay: Duration) : SubintentExpiration {

        override fun toDAppInteraction(): DappToWalletInteractionSubintentExpiration =
            DappToWalletInteractionSubintentExpiration.AfterDelay(
                DappToWalletInteractionSubintentExpireAfterDelay(expireAfterSeconds = delay.inWholeSeconds.toULong())
            )

        override fun expirationTimestamp(): Timestamp =
            Timestamp.ofInstant(Instant.now().plusMillis(delay.inWholeMilliseconds), ZoneId.systemDefault())
    }

    companion object {

        fun from(expiration: DappToWalletInteractionSubintentExpiration) = when (expiration) {
            is DappToWalletInteractionSubintentExpiration.AtTime -> AtTime(
                expireAt = expiration.v1.unixTimestampSeconds.toLong().seconds
            )

            is DappToWalletInteractionSubintentExpiration.AfterDelay -> DelayAfterSign(
                delay = expiration.v1.expireAfterSeconds.toLong().seconds
            )
        }
    }
}
