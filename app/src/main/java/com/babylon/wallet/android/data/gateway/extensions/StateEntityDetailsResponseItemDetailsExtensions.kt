@file:Suppress("CommentSpacing", "UnusedPrivateMember", "NoUnusedImports", "TooManyFunctions")

package com.babylon.wallet.android.data.gateway.extensions

import com.babylon.wallet.android.data.gateway.generated.models.FungibleResourcesCollectionItemVaultAggregated
import com.babylon.wallet.android.data.gateway.generated.models.StateEntityDetailsResponseComponentDetails
import com.babylon.wallet.android.data.gateway.generated.models.StateEntityDetailsResponseFungibleResourceDetails
import com.babylon.wallet.android.data.gateway.generated.models.StateEntityDetailsResponseItem
import com.babylon.wallet.android.data.gateway.generated.models.StateEntityDetailsResponseItemDetails
import com.babylon.wallet.android.data.gateway.generated.models.StateEntityDetailsResponseNonFungibleResourceDetails
import com.babylon.wallet.android.domain.model.XrdResource
import com.babylon.wallet.android.domain.model.behaviours.AssetBehaviours
import java.math.BigDecimal

fun StateEntityDetailsResponseItemDetails.totalSupply(): String? {
    return when (val details = this) {
        is StateEntityDetailsResponseFungibleResourceDetails -> details.totalSupply
        is StateEntityDetailsResponseNonFungibleResourceDetails -> details.totalSupply
        else -> null
    }
}

fun StateEntityDetailsResponseItemDetails.divisibility(): Int? {
    return when (val details = this) {
        is StateEntityDetailsResponseFungibleResourceDetails -> details.divisibility
        else -> null
    }
}

fun StateEntityDetailsResponseItem.getXRDVaultAmount(vaultAddress: String): BigDecimal? {
    return when (
        val resource = fungibleResources?.items?.find {
            XrdResource.officialAddresses.contains(it.resourceAddress)
        }
    ) {
        is FungibleResourcesCollectionItemVaultAggregated -> {
            resource.vaults.items.find { it.vaultAddress == vaultAddress }?.amount?.toBigDecimal()
        }

        else -> null
    }
}

fun StateEntityDetailsResponseItemDetails.xrdVaultAddress(): String? {
    return when (val details = this) {
        is StateEntityDetailsResponseComponentDetails -> details.state?.stakeXrdVault?.entityAddress
        else -> null
    }
}

fun StateEntityDetailsResponseItemDetails.stakeUnitResourceAddress(): String? {
    return when (val details = this) {
        is StateEntityDetailsResponseComponentDetails -> details.state?.stakeUnitResourceAddress
        else -> null
    }
}

val StateEntityDetailsResponseItemDetails.claimTokenResourceAddress: String?
    get() {
        return when (val details = this) {
            is StateEntityDetailsResponseComponentDetails -> details.state?.claimTokenResourceAddress
            else -> null
        }
    }

fun StateEntityDetailsResponseItemDetails.extractBehaviours(): AssetBehaviours = when (val details = this) {
    is StateEntityDetailsResponseFungibleResourceDetails -> details.roleAssignments.assetBehaviours()
    is StateEntityDetailsResponseNonFungibleResourceDetails -> details.roleAssignments.assetBehaviours()
    else -> setOf()
}
