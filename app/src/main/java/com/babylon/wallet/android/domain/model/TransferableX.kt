package com.babylon.wallet.android.domain.model

import com.radixdlt.sargon.Decimal192
import com.radixdlt.sargon.NonFungibleLocalId
import com.radixdlt.sargon.ResourceAddress
import com.radixdlt.sargon.extensions.times
import com.radixdlt.sargon.extensions.toDecimal192
import rdx.works.core.domain.assets.Asset
import rdx.works.core.domain.assets.LiquidStakeUnit
import rdx.works.core.domain.assets.NonFungibleCollection
import rdx.works.core.domain.resources.Resource

sealed interface TransferableX {

    val asset: Asset
    val amount: Amount // TODO might not needed
    val isNewlyCreated: Boolean

    val resourceAddress: ResourceAddress
        get() = asset.resource.address

    sealed interface FungibleType : TransferableX {

        override val amount: FungibleAmount

        data class Token(
            override val asset: rdx.works.core.domain.assets.Token,
            override val amount: FungibleAmount,
            override val isNewlyCreated: Boolean
        ) : FungibleType

        data class LSU(
            override val asset: LiquidStakeUnit,
            override val amount: FungibleAmount,
            override val isNewlyCreated: Boolean = false,
            val xrdWorth: Decimal192 // TODO not sure this is the right place
        ) : FungibleType

        data class PoolUnit(
            override val asset: rdx.works.core.domain.assets.PoolUnit,
            override val amount: FungibleAmount,
            override val isNewlyCreated: Boolean = false,
            val contributionPerResource: Map<ResourceAddress, Decimal192>,
        ) : FungibleType
    }

    sealed interface NonFungibleType : TransferableX {

        override val amount: NonFungibleAmount

        data class NFTCollection(
            override val asset: NonFungibleCollection,
            override val amount: NonFungibleAmount,
            override val isNewlyCreated: Boolean
        ) : NonFungibleType

        data class StakeClaim(
            override val asset: rdx.works.core.domain.assets.StakeClaim,
            override val amount: NonFungibleAmount,
            val xrdWorthPerNftItem: Map<NonFungibleLocalId, Decimal192>,// TODO not sure this is the right place
            override val isNewlyCreated: Boolean = false
        ) : NonFungibleType
    }
}

sealed interface Amount // TODO might not needed

sealed interface FungibleAmount : Amount {

    data class Exact(val amount: Decimal192) : FungibleAmount

    data class Range(
        val minAmount: Decimal192,
        val maxAmount: Decimal192
    ) : FungibleAmount

    data class Min(val amount: Decimal192) : FungibleAmount

    data class Max(val amount: Decimal192) : FungibleAmount

    data class Predicted(
        val amount: Decimal192,
        val instructionIndex: Long,
        val guaranteeOffset: Decimal192
    ) : FungibleAmount {

        val guaranteeAmount: Decimal192
            get() = amount * guaranteeOffset

        @Suppress("MagicNumber")
        val guaranteePercent: Decimal192
            get() = guaranteeOffset * 100.toDecimal192()
    }

    data object Unknown : FungibleAmount
}

sealed interface NonFungibleAmount : Amount { // TODO under research

    data class Exact(val nftItem: Resource.NonFungibleResource.Item) : NonFungibleAmount

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
