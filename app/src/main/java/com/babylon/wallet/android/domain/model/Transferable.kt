package com.babylon.wallet.android.domain.model

import androidx.annotation.FloatRange
import com.babylon.wallet.android.domain.model.assets.LiquidStakeUnit
import com.babylon.wallet.android.domain.model.assets.PoolUnit
import com.babylon.wallet.android.domain.model.assets.ValidatorDetail
import com.babylon.wallet.android.domain.model.resources.Resource
import java.math.BigDecimal

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
                            amount = transferable.amount * predicted.guaranteeOffset.toBigDecimal(),
                            instructionIndex = predicted.instructionIndex
                        )
                        is TransferableAsset.Fungible.PoolUnitAsset -> GuaranteeAssertion.ForAmount(
                            amount = transferable.amount * predicted.guaranteeOffset.toBigDecimal(),
                            instructionIndex = predicted.instructionIndex
                        )
                        is TransferableAsset.Fungible.LSUAsset -> GuaranteeAssertion.ForAmount(
                            amount = transferable.amount * predicted.guaranteeOffset.toBigDecimal(),
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
        @FloatRange(from = 0.0)
        guaranteeOffset: Double
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
        val amount: BigDecimal,
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
        @FloatRange(from = 0.0, to = 1.0)
        val guaranteeOffset: Double
    ) : GuaranteeType {

        @Suppress("MagicNumber")
        val guaranteePercent: Double
            get() = guaranteeOffset * 100
    }
}

sealed interface TransferableAsset {

    val resource: Resource
    val resourceAddress: String
        get() = resource.resourceAddress
    val isNewlyCreated: Boolean

    sealed class Fungible : TransferableAsset {
        abstract val amount: BigDecimal
        data class Token(
            override val amount: BigDecimal,
            override val resource: Resource.FungibleResource,
            override val isNewlyCreated: Boolean
        ) : Fungible()

        data class LSUAsset(
            override val amount: BigDecimal,
            val lsu: LiquidStakeUnit,
            val validator: ValidatorDetail,
            val xrdWorth: BigDecimal,
            override val isNewlyCreated: Boolean = false
        ) : Fungible() {
            override val resource: Resource.FungibleResource
                get() = lsu.fungibleResource
        }

        data class PoolUnitAsset(
            override val amount: BigDecimal,
            val unit: PoolUnit,
            val contributionPerResource: Map<String, BigDecimal>,
            val associatedDapp: DApp?,
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
            override val resource: Resource.NonFungibleResource,
            val validator: ValidatorDetail,
            val xrdWorthPerNftItem: Map<String, BigDecimal>,
            override val isNewlyCreated: Boolean = false
        ) : NonFungible()
    }
}
