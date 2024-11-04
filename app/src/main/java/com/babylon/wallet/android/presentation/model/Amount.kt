package com.babylon.wallet.android.presentation.model

import com.radixdlt.sargon.Decimal192
import com.radixdlt.sargon.extensions.times
import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import rdx.works.core.domain.resources.Resource

sealed interface Amount // TODO might not needed

@Serializable
sealed interface FungibleAmount : Amount {

    @Serializable
    data class Exact(@Contextual val amount: Decimal192) : FungibleAmount {
        override fun calculateWith(calculation: (Decimal192) -> Decimal192): FungibleAmount = Exact(amount = calculation(amount))
    }

    @Serializable
    data class Range(
        @Contextual val minAmount: Decimal192,
        @Contextual val maxAmount: Decimal192
    ) : FungibleAmount {
        override fun calculateWith(calculation: (Decimal192) -> Decimal192): FungibleAmount = Range(
            minAmount = calculation(minAmount),
            maxAmount = calculation(maxAmount)
        )
    }

    @Serializable
    data class Min(@Contextual val amount: Decimal192) : FungibleAmount {
        override fun calculateWith(calculation: (Decimal192) -> Decimal192): FungibleAmount = Min(amount = calculation(amount))
    }

    @Serializable
    data class Max(@Contextual val amount: Decimal192) : FungibleAmount {
        override fun calculateWith(calculation: (Decimal192) -> Decimal192): FungibleAmount = Max(amount = calculation(amount))
    }

    @Serializable
    data class Predicted(
        @Contextual val estimated: Decimal192,
        val instructionIndex: Long,
        @Contextual val offset: Decimal192
    ) : FungibleAmount {
        val guaranteed: Decimal192
            get() = estimated * offset

        override fun calculateWith(calculation: (Decimal192) -> Decimal192): FungibleAmount = copy(
            estimated = calculation(estimated)
        )
    }

    @Serializable
    data object Unknown : FungibleAmount {
        override fun calculateWith(calculation: (Decimal192) -> Decimal192): FungibleAmount = Unknown
    }

    fun just(decimal: Decimal192): FungibleAmount = calculateWith { decimal }

    fun calculateWith(calculation: (Decimal192) -> Decimal192): FungibleAmount
}

sealed interface NonFungibleAmount : Amount {

    // TODO may need to remove this
    val certainNFTs: List<Resource.NonFungibleResource.Item>

    data class Certain(val nfts: List<Resource.NonFungibleResource.Item>) : NonFungibleAmount {
        override val certainNFTs: List<Resource.NonFungibleResource.Item>
            get() = nfts
    }

    data class NotExact(
        val certain: List<Resource.NonFungibleResource.Item>,
        val additional: NonFungibleAmountBounds?
    ) : NonFungibleAmount {

        override val certainNFTs: List<Resource.NonFungibleResource.Item>
            get() = certain
    }
}

sealed interface NonFungibleAmountBounds {
    @Serializable
    data class Exact(
        @Contextual val amount: Decimal192,
    ) : NonFungibleAmountBounds

    @Serializable
    data class Range(
        @Contextual val minAmount: Decimal192,
        @Contextual val maxAmount: Decimal192
    ) : NonFungibleAmountBounds

    @Serializable
    data class Min(@Contextual val amount: Decimal192) : NonFungibleAmountBounds

    @Serializable
    data class Max(@Contextual val amount: Decimal192) : NonFungibleAmountBounds

    @Serializable
    data object Unknown: NonFungibleAmountBounds
}
