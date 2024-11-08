package com.babylon.wallet.android.presentation.transaction.model

import com.babylon.wallet.android.presentation.model.BoundedAmount
import com.babylon.wallet.android.presentation.model.NonFungibleAmount
import com.radixdlt.sargon.ResourceAddress
import com.radixdlt.sargon.extensions.orZero
import rdx.works.core.domain.assets.Asset
import rdx.works.core.domain.assets.LiquidStakeUnit
import rdx.works.core.domain.assets.NonFungibleCollection

sealed interface Transferable {

    val asset: Asset
    val isNewlyCreated: Boolean

    val resourceAddress: ResourceAddress
        get() = asset.resource.address

    sealed interface FungibleType : Transferable {

        val amount: BoundedAmount

        data class Token(
            override val asset: rdx.works.core.domain.assets.Token,
            override val amount: BoundedAmount,
            override val isNewlyCreated: Boolean = false
        ) : FungibleType

        data class LSU(
            override val asset: LiquidStakeUnit,
            override val amount: BoundedAmount,
            override val isNewlyCreated: Boolean = false,
            val xrdWorth: BoundedAmount
        ) : FungibleType

        data class PoolUnit(
            override val asset: rdx.works.core.domain.assets.PoolUnit,
            override val amount: BoundedAmount,
            override val isNewlyCreated: Boolean = false,
            val contributions: Map<ResourceAddress, BoundedAmount> = asset.pool?.resources.orEmpty().associate { poolItem ->
                poolItem.address to amount.calculateWith { decimal ->
                    asset.poolItemRedemptionValue(address = poolItem.address, poolUnitAmount = decimal).orZero()
                }
            }
        ) : FungibleType
    }

    sealed interface NonFungibleType : Transferable {

        val amount: NonFungibleAmount

        data class NFTCollection(
            override val asset: NonFungibleCollection,
            override val amount: NonFungibleAmount,
            override val isNewlyCreated: Boolean = false
        ) : NonFungibleType

        data class StakeClaim(
            override val asset: rdx.works.core.domain.assets.StakeClaim,
            override val amount: NonFungibleAmount,
            override val isNewlyCreated: Boolean = false
        ) : NonFungibleType
    }
}
