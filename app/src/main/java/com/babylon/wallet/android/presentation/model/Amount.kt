package com.babylon.wallet.android.presentation.model

import com.radixdlt.sargon.Decimal192
import com.radixdlt.sargon.extensions.times
import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import rdx.works.core.domain.resources.Resource

sealed interface Amount

data class NonFungibleAmount(
    val certain: List<Resource.NonFungibleResource.Item>,
    val additional: CountedAmount? = null
) : Amount

@Serializable
sealed interface CountedAmount {

    @Serializable
    data class Exact(@Contextual val amount: Decimal192) : CountedAmount {
        override fun calculateWith(calculation: (Decimal192) -> Decimal192): CountedAmount = Exact(amount = calculation(amount))
    }

    @Serializable
    data class Range(
        @Contextual val minAmount: Decimal192,
        @Contextual val maxAmount: Decimal192
    ) : CountedAmount {
        override fun calculateWith(calculation: (Decimal192) -> Decimal192): CountedAmount = Range(
            minAmount = calculation(minAmount),
            maxAmount = calculation(maxAmount)
        )
    }

    @Serializable
    data class Min(@Contextual val amount: Decimal192) : CountedAmount {
        override fun calculateWith(calculation: (Decimal192) -> Decimal192): CountedAmount = Min(amount = calculation(amount))
    }

    @Serializable
    data class Max(@Contextual val amount: Decimal192) : CountedAmount {
        override fun calculateWith(calculation: (Decimal192) -> Decimal192): CountedAmount = Max(amount = calculation(amount))
    }

    @Serializable
    data class Predicted(
        @Contextual val estimated: Decimal192,
        val instructionIndex: Long,
        @Contextual val offset: Decimal192
    ) : CountedAmount {

        val guaranteed: Decimal192
            get() = estimated * offset

        override fun calculateWith(calculation: (Decimal192) -> Decimal192): CountedAmount = copy(
            estimated = calculation(estimated)
        )
    }

    @Serializable
    data object Unknown : CountedAmount {
        override fun calculateWith(calculation: (Decimal192) -> Decimal192): CountedAmount = Unknown
    }

    fun just(decimal: Decimal192): CountedAmount = calculateWith { decimal }

    fun calculateWith(calculation: (Decimal192) -> Decimal192): CountedAmount
}
