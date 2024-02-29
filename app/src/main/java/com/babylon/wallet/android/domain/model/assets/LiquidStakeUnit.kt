package com.babylon.wallet.android.domain.model.assets

import android.net.Uri
import rdx.works.core.domain.resources.Resource
import rdx.works.core.divideWithDivisibility
import rdx.works.core.multiplyWithDivisibility
import java.math.BigDecimal

data class LiquidStakeUnit(
    val fungibleResource: Resource.FungibleResource,
    val validator: ValidatorDetail
) : Asset.Fungible {

    override val resource: Resource.FungibleResource
        get() = fungibleResource

    val validatorAddress: String
        get() = fungibleResource.validatorAddress.orEmpty()
    val resourceAddress: String
        get() = fungibleResource.resourceAddress

    val name: String
        get() = fungibleResource.name

    val iconUrl: Uri?
        get() = fungibleResource.iconUrl

    private val percentageOwned: BigDecimal?
        get() {
            if (fungibleResource.currentSupply == null) return null

            return fungibleResource.ownedAmount?.divideWithDivisibility(
                fungibleResource.currentSupply,
                fungibleResource.divisibility
            )
        }

    fun stakeValue(): BigDecimal? = stakeValueInXRD(validator.totalXrdStake)

    fun stakeValueInXRD(totalXrdStake: BigDecimal?): BigDecimal? {
        if (totalXrdStake == null) return null
        return percentageOwned?.multiplyWithDivisibility(totalXrdStake, fungibleResource.divisibility)
    }
}
