package com.babylon.wallet.android.domain.model

import com.radixdlt.sargon.Decimal192
import com.radixdlt.sargon.NonFungibleLocalId
import com.radixdlt.sargon.ResourceAddress
import com.radixdlt.sargon.extensions.times
import com.radixdlt.sargon.extensions.toDecimal192
import rdx.works.core.domain.assets.LiquidStakeUnit
import rdx.works.core.domain.assets.PoolUnit
import rdx.works.core.domain.assets.StakeClaim
import rdx.works.core.domain.resources.Resource

sealed interface Transferable {
    val transferable: TransferableAsset

    /**
     * Will exist only for
     * 1. depositing trasnferrables
     * 2. with guarantee type Predicted
     * 3. is not newly created
     */
    val guaranteeAssertion: GuaranteeAssertion?
        get() {
            return when (this) {
                is Depositing -> {
                    if (transferable.isNewlyCreated) return null
                    val predicted = guaranteeType as? GuaranteeType.Predicted ?: return null

                    when (val transferable = transferable) {
                        is TransferableAsset.Fungible.Token -> GuaranteeAssertion.ForAmount(
                            amount = transferable.amount * predicted.guaranteeOffset,
                            instructionIndex = predicted.instructionIndex
                        )
                        is TransferableAsset.Fungible.PoolUnitAsset -> GuaranteeAssertion.ForAmount(
                            amount = transferable.amount * predicted.guaranteeOffset,
                            instructionIndex = predicted.instructionIndex
                        )
                        is TransferableAsset.Fungible.LSUAsset -> GuaranteeAssertion.ForAmount(
                            amount = transferable.amount * predicted.guaranteeOffset,
                            instructionIndex = predicted.instructionIndex
                        )
                        is TransferableAsset.NonFungible.NFTAssets -> GuaranteeAssertion.ForNFT(
                            instructionIndex = predicted.instructionIndex
                        )
                        is TransferableAsset.NonFungible.StakeClaimAssets -> GuaranteeAssertion.ForNFT(
                            instructionIndex = predicted.instructionIndex
                        )
                    }
                }

                is Withdrawing -> null
            }
        }

    val hasEditableGuarantees: Boolean
        get() {
            return when (this) {
                is Depositing -> guaranteeType is GuaranteeType.Predicted && !transferable.isNewlyCreated
                is Withdrawing -> false
            }
        }

    data class Depositing(
        override val transferable: TransferableAsset,
        val guaranteeType: GuaranteeType = GuaranteeType.Guaranteed
    ) : Transferable

    data class Withdrawing(
        override val transferable: TransferableAsset
    ) : Transferable

    fun updateGuarantee(
        guaranteeOffset: Decimal192
    ): Transferable {
        return when (this) {
            is Depositing -> {
                val predicted = (guaranteeType as? GuaranteeType.Predicted) ?: return this

                copy(guaranteeType = predicted.copy(guaranteeOffset = guaranteeOffset))
            }

            is Withdrawing -> this
        }
    }
}

sealed interface GuaranteeAssertion {
    val instructionIndex: Long

    data class ForAmount(
        val amount: Decimal192,
        override val instructionIndex: Long
    ) : GuaranteeAssertion

    data class ForNFT(
        override val instructionIndex: Long
    ) : GuaranteeAssertion
}

sealed interface GuaranteeType {
    data object Guaranteed : GuaranteeType
    data class Predicted(
        val instructionIndex: Long,
        val guaranteeOffset: Decimal192
    ) : GuaranteeType {

        @Suppress("MagicNumber")
        val guaranteePercent: Decimal192
            get() = guaranteeOffset * 100.toDecimal192()
    }
}

sealed interface TransferableAsset {

    val resource: Resource
    val resourceAddress: ResourceAddress
        get() = resource.address
    val isNewlyCreated: Boolean

    sealed class Fungible : TransferableAsset {
        abstract val amount: Decimal192
        data class Token(
            override val amount: Decimal192,
            override val resource: Resource.FungibleResource,
            override val isNewlyCreated: Boolean
        ) : Fungible()

        data class LSUAsset(
            override val amount: Decimal192,
            val lsu: LiquidStakeUnit,
            val xrdWorth: Decimal192,
            override val isNewlyCreated: Boolean = false
        ) : Fungible() {
            override val resource: Resource.FungibleResource
                get() = lsu.fungibleResource
        }

        data class PoolUnitAsset(
            override val amount: Decimal192,
            val unit: PoolUnit,
            val contributionPerResource: Map<ResourceAddress, Decimal192>,
            override val isNewlyCreated: Boolean = false
        ) : Fungible() {
            override val resource: Resource.FungibleResource
                get() = unit.stake
        }
    }

    sealed class NonFungible : TransferableAsset {
        data class NFTAssets(
            override val resource: Resource.NonFungibleResource,
            override val isNewlyCreated: Boolean
        ) : NonFungible()

        data class StakeClaimAssets(
            val claim: StakeClaim,
            val xrdWorthPerNftItem: Map<NonFungibleLocalId, Decimal192>,
            override val isNewlyCreated: Boolean = false
        ) : NonFungible() {
            override val resource: Resource.NonFungibleResource
                get() = claim.nonFungibleResource
        }
    }
}
