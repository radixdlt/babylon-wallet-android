package com.babylon.wallet.android.presentation.transaction.model

import com.babylon.wallet.android.presentation.model.Amount
import com.babylon.wallet.android.presentation.model.FungibleAmount
import com.babylon.wallet.android.presentation.model.NonFungibleAmount
import com.radixdlt.sargon.Decimal192
import com.radixdlt.sargon.NonFungibleLocalId
import com.radixdlt.sargon.ResourceAddress
import rdx.works.core.domain.assets.Asset
import rdx.works.core.domain.assets.LiquidStakeUnit
import rdx.works.core.domain.assets.NonFungibleCollection

sealed interface Transferable {

    val asset: Asset
    val amount: Amount
    val isNewlyCreated: Boolean

    val resourceAddress: ResourceAddress
        get() = asset.resource.address

    sealed interface FungibleType : Transferable {

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
            val contributionPerResource: Map<ResourceAddress, FungibleAmount>,
        ) : FungibleType
    }

    sealed interface NonFungibleType : Transferable {

        override val amount: NonFungibleAmount

        data class NFTCollection(
            override val asset: NonFungibleCollection,
            override val amount: NonFungibleAmount,
            override val isNewlyCreated: Boolean
        ) : NonFungibleType

        data class StakeClaim(
            override val asset: rdx.works.core.domain.assets.StakeClaim,
            override val amount: NonFungibleAmount,
            val xrdWorthPerNftItem: Map<NonFungibleLocalId, Decimal192>,
            override val isNewlyCreated: Boolean = false
        ) : NonFungibleType
    }
}