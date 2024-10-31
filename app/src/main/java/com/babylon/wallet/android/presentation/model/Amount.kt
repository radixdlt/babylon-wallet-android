package com.babylon.wallet.android.presentation.model

import com.radixdlt.sargon.Decimal192
import com.radixdlt.sargon.NonFungibleLocalId
import com.radixdlt.sargon.extensions.times
import com.radixdlt.sargon.extensions.toDecimal192
import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import rdx.works.core.domain.resources.Resource

sealed interface Amount // TODO might not needed

@Serializable
sealed interface FungibleAmount : Amount {

    @Serializable
    data class Exact(@Contextual val amount: Decimal192) : FungibleAmount

    @Serializable
    data class Range(
        @Contextual val minAmount: Decimal192,
        @Contextual val maxAmount: Decimal192
    ) : FungibleAmount

    @Serializable
    data class Min(@Contextual val amount: Decimal192) : FungibleAmount

    @Serializable
    data class Max(@Contextual val amount: Decimal192) : FungibleAmount

    @Serializable
    data class Predicted(
        @Contextual val amount: Decimal192,
        val instructionIndex: Long,
        @Contextual val guaranteeOffset: Decimal192
    ) : FungibleAmount {

        val guaranteeAmount: Decimal192
            get() = amount * guaranteeOffset

        @Suppress("MagicNumber")
        val guaranteePercent: Decimal192
            get() = guaranteeOffset * 100.toDecimal192()
    }

    @Serializable
    data object Unknown : FungibleAmount
}

sealed interface NonFungibleAmount : Amount { // TODO under research

    data class Exact(val nfts: List<Resource.NonFungibleResource.Item>) : NonFungibleAmount

    data class NotExact(
        val lowerBound: LowerBound,
        val upperBound: UpperBound,
    ) : NonFungibleAmount {

        sealed interface LowerBound {
            data object NonZero : LowerBound
            data class Inclusive(val amount: Decimal192) : LowerBound
        }

        sealed interface UpperBound {
            data class Inclusive(val amount: Decimal192) : UpperBound
            data object Unbounded : UpperBound
        }
    }
}
