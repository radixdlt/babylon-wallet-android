@file:Suppress("CommentSpacing", "UnusedPrivateMember", "NoUnusedImports", "TooManyFunctions")

package com.babylon.wallet.android.data.gateway.extensions

import com.babylon.wallet.android.data.gateway.generated.models.FungibleResourcesCollectionItemVaultAggregated
import com.babylon.wallet.android.data.gateway.generated.models.StateEntityDetailsResponseComponentDetails
import com.babylon.wallet.android.data.gateway.generated.models.StateEntityDetailsResponseFungibleResourceDetails
import com.babylon.wallet.android.data.gateway.generated.models.StateEntityDetailsResponseItem
import com.babylon.wallet.android.data.gateway.generated.models.StateEntityDetailsResponseItemDetails
import com.babylon.wallet.android.data.gateway.generated.models.StateEntityDetailsResponseNonFungibleResourceDetails
import com.radixdlt.sargon.Decimal192
import com.radixdlt.sargon.VaultAddress
import com.radixdlt.sargon.extensions.init
import com.radixdlt.sargon.extensions.networkId
import com.radixdlt.sargon.extensions.string
import com.radixdlt.sargon.extensions.toDecimal192OrNull
import rdx.works.core.domain.assets.AssetBehaviours
import rdx.works.core.domain.resources.Divisibility
import rdx.works.core.domain.resources.XrdResource

fun StateEntityDetailsResponseItemDetails.totalSupply(): Decimal192? {
    return when (val details = this) {
        is StateEntityDetailsResponseFungibleResourceDetails -> details.totalSupply.toDecimal192OrNull()
        is StateEntityDetailsResponseNonFungibleResourceDetails -> details.totalSupply.toDecimal192OrNull()
        else -> null
    }
}

fun StateEntityDetailsResponseItemDetails.divisibility(): Divisibility? {
    return when (val details = this) {
        is StateEntityDetailsResponseFungibleResourceDetails -> Divisibility(details.divisibility.toUByte())
        else -> null
    }
}

val StateEntityDetailsResponseItemDetails.xrdVaultAddress: String?
    get() = when (val details = this) {
        is StateEntityDetailsResponseComponentDetails -> details.state?.stakeXrdVault?.entityAddress
        else -> null
    }

val StateEntityDetailsResponseItem.totalXRDStake: Decimal192?
    get() {
        val xrdVaultAddress = details?.xrdVaultAddress?.let { runCatching { VaultAddress.init(it) }.getOrNull() } ?: return null

        val xrdResource = fungibleResources?.items?.find {
            XrdResource.address(networkId = xrdVaultAddress.networkId).string == it.resourceAddress
        }

        return if (xrdResource is FungibleResourcesCollectionItemVaultAggregated) {
            xrdResource.vaults.items.find { it.vaultAddress == xrdVaultAddress.string }?.amountDecimal
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
