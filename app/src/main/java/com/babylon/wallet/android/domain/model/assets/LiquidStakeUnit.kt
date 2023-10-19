package com.babylon.wallet.android.domain.model.assets

import android.net.Uri
import com.babylon.wallet.android.domain.model.resources.Resource
import java.math.BigDecimal

data class LiquidStakeUnit(
    val fungibleResource: Resource.FungibleResource
) {

    val validatorAddress: String
        get() = fungibleResource.validatorAddress.orEmpty()

    val resourceAddress: String
        get() = fungibleResource.resourceAddress
    val name: String
        get() = fungibleResource.name

    val iconUrl: Uri?
        get() = fungibleResource.iconUrl

    private val percentageOwned: BigDecimal?
        get() = fungibleResource.ownedAmount?.divide(fungibleResource.currentSupply, fungibleResource.mathContext)

    fun stakeValueInXRD(totalXrdStake: BigDecimal?): BigDecimal? {
        return percentageOwned?.multiply(totalXrdStake, fungibleResource.mathContext)
    }
}
