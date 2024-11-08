package com.babylon.wallet.android.presentation.model

import com.radixdlt.sargon.Decimal192
import com.radixdlt.sargon.extensions.times
import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import rdx.works.core.domain.resources.Resource

data class NonFungibleAmount(
    val certain: List<Resource.NonFungibleResource.Item>,
    val additional: BoundedAmount? = null
) {

    init {
        additional?.let {
            require(it !is BoundedAmount.Predicted) { "Predicted additional amount not supported for non-fungibles" }
        }
    }
}

@Serializable
sealed interface BoundedAmount {

    @Serializable
    data class Exact(@Contextual val amount: Decimal192) : BoundedAmount {
        override fun calculateWith(calculation: (Decimal192) -> Decimal192): BoundedAmount = Exact(amount = calculation(amount))
    }

    @Serializable
    data class Range(
        @Contextual val minAmount: Decimal192,
        @Contextual val maxAmount: Decimal192
    ) : BoundedAmount {
        override fun calculateWith(calculation: (Decimal192) -> Decimal192): BoundedAmount = Range(
            minAmount = calculation(minAmount),
            maxAmount = calculation(maxAmount)
        )
    }

    @Serializable
    data class Min(@Contextual val amount: Decimal192) : BoundedAmount {
        override fun calculateWith(calculation: (Decimal192) -> Decimal192): BoundedAmount = Min(amount = calculation(amount))
    }

    @Serializable
    data class Max(@Contextual val amount: Decimal192) : BoundedAmount {
        override fun calculateWith(calculation: (Decimal192) -> Decimal192): BoundedAmount = Max(amount = calculation(amount))
    }

    @Serializable
    data class Predicted(
        @Contextual val estimated: Decimal192,
        val instructionIndex: Long,
        @Contextual val offset: Decimal192
    ) : BoundedAmount {

        val guaranteed: Decimal192
            get() = estimated * offset

        override fun calculateWith(calculation: (Decimal192) -> Decimal192): BoundedAmount = copy(
            estimated = calculation(estimated)
        )
    }

    @Serializable
    data object Unknown : BoundedAmount {
        override fun calculateWith(calculation: (Decimal192) -> Decimal192): BoundedAmount = Unknown
    }

    fun just(decimal: Decimal192): BoundedAmount = calculateWith { decimal }

    fun calculateWith(calculation: (Decimal192) -> Decimal192): BoundedAmount
}
