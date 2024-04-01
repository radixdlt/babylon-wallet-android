@file:Suppress("CommentSpacing", "UnusedPrivateMember", "NoUnusedImports", "TooManyFunctions")

package com.babylon.wallet.android.data.gateway.extensions

import com.babylon.wallet.android.data.gateway.generated.models.FungibleResourcesCollectionItemVaultAggregated
import com.babylon.wallet.android.data.gateway.generated.models.StateEntityDetailsResponseComponentDetails
import com.babylon.wallet.android.data.gateway.generated.models.StateEntityDetailsResponseFungibleResourceDetails
import com.babylon.wallet.android.data.gateway.generated.models.StateEntityDetailsResponseItem
import com.babylon.wallet.android.data.gateway.generated.models.StateEntityDetailsResponseItemDetails
import com.babylon.wallet.android.data.gateway.generated.models.StateEntityDetailsResponseNonFungibleResourceDetails
import com.radixdlt.sargon.VaultAddress
import com.radixdlt.sargon.extensions.discriminant
import com.radixdlt.sargon.extensions.init
import com.radixdlt.sargon.extensions.networkId
import com.radixdlt.sargon.extensions.string
import rdx.works.core.AddressHelper
import rdx.works.core.domain.assets.AssetBehaviours
import rdx.works.core.domain.resources.XrdResource
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

val StateEntityDetailsResponseItemDetails.xrdVaultAddress: String?
    get() = when (val details = this) {
        is StateEntityDetailsResponseComponentDetails -> details.state?.stakeXrdVault?.entityAddress
        else -> null
    }

val StateEntityDetailsResponseItem.totalXRDStake: BigDecimal?
    get() {
        val xrdVaultAddress = details?.xrdVaultAddress?.let { runCatching { VaultAddress.init(it) }.getOrNull() } ?: return null

        val xrdResource = fungibleResources?.items?.find {
            XrdResource.address(networkId = xrdVaultAddress.networkId.discriminant.toInt()).string == it.resourceAddress
        }

        return if (xrdResource is FungibleResourcesCollectionItemVaultAggregated) {
            xrdResource.vaults.items.find { it.vaultAddress == xrdVaultAddress.string }?.amount?.toBigDecimal()
        } else {
            null
        }
    }

val StateEntityDetailsResponseItemDetails.stakeUnitResourceAddress: String?
    get() = when (this) {
        is StateEntityDetailsResponseComponentDetails -> state?.stakeUnitResourceAddress
        else -> null
    }

val StateEntityDetailsResponseItemDetails.claimTokenResourceAddress: String?
    get() = when (this) {
        is StateEntityDetailsResponseComponentDetails -> state?.claimTokenResourceAddress
        else -> null
    }

fun StateEntityDetailsResponseItemDetails.extractBehaviours(): AssetBehaviours = when (val details = this) {
    is StateEntityDetailsResponseFungibleResourceDetails -> details.roleAssignments.assetBehaviours()
    is StateEntityDetailsResponseNonFungibleResourceDetails -> details.roleAssignments.assetBehaviours()
    else -> setOf()
}
