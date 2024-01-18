package com.babylon.wallet.android.domain.model

import androidx.annotation.FloatRange
import com.babylon.wallet.android.domain.model.assets.PoolUnit
import com.babylon.wallet.android.domain.model.assets.ValidatorDetail
import com.babylon.wallet.android.domain.model.resources.Resource
import java.math.BigDecimal

sealed interface Transferable {
    val transferable: TransferableResource

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
                        is TransferableResource.FungibleAmount -> GuaranteeAssertion.ForAmount(
                            amount = transferable.amount * predicted.guaranteeOffset.toBigDecimal(),
                            instructionIndex = predicted.instructionIndex
                        )

                        is TransferableResource.NFTs -> GuaranteeAssertion.ForNFT(
                            instructionIndex = predicted.instructionIndex
                        )

                        is TransferableResource.LsuAmount -> GuaranteeAssertion.ForAmount(
                            amount = transferable.amount * predicted.guaranteeOffset.toBigDecimal(),
                            instructionIndex = predicted.instructionIndex
                        )

                        is TransferableResource.StakeClaimNft -> GuaranteeAssertion.ForNFT(
                            instructionIndex = predicted.instructionIndex
                        )

                        is TransferableResource.PoolUnitAmount -> GuaranteeAssertion.ForAmount(
                            amount = transferable.amount * predicted.guaranteeOffset.toBigDecimal(),
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
        override val transferable: TransferableResource,
        val guaranteeType: GuaranteeType = GuaranteeType.Guaranteed
    ) : Transferable

    data class Withdrawing(
        override val transferable: TransferableResource
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

sealed interface TransferableResource {

    val resource: Resource
    val resourceAddress: String
        get() = resource.resourceAddress
    val isNewlyCreated: Boolean

    data class FungibleAmount(
        override val amount: BigDecimal,
        override val resource: Resource.FungibleResource,
        override val isNewlyCreated: Boolean
    ) : TransferableResource, TransferableWithGuarantees {
        override val fungibleResource: Resource.FungibleResource
            get() = resource
    }

    data class LsuAmount(
        override val amount: BigDecimal,
        override val resource: Resource.FungibleResource,
        val validatorDetail: ValidatorDetail,
        val xrdWorth: BigDecimal,
        override val isNewlyCreated: Boolean = false
    ) : TransferableResource, TransferableWithGuarantees {
        override val fungibleResource: Resource.FungibleResource
            get() = resource
    }

    data class NFTs(
        override val resource: Resource.NonFungibleResource,
        override val isNewlyCreated: Boolean
    ) : TransferableResource

    data class StakeClaimNft(
        override val resource: Resource.NonFungibleResource,
        val xrdWorthPerNftItem: Map<String, BigDecimal>,
        val validatorDetail: ValidatorDetail,
        override val isNewlyCreated: Boolean = false
    ) : TransferableResource

    data class PoolUnitAmount(
        override val amount: BigDecimal,
        val poolUnit: PoolUnit,
        val contributionPerResource: Map<String, BigDecimal>,
        val associatedDapp: DApp?,
        override val isNewlyCreated: Boolean = false,
    ) : TransferableResource, TransferableWithGuarantees {
        override val resource: Resource
            get() = poolUnit.stake
        override val fungibleResource: Resource.FungibleResource
            get() = poolUnit.stake
    }
}

interface TransferableWithGuarantees {
    val fungibleResource: Resource.FungibleResource
    val amount: BigDecimal
}
