package com.babylon.wallet.android.data.gateway.extensions

import com.babylon.wallet.android.data.gateway.generated.models.DefaultDepositRule
import com.babylon.wallet.android.data.gateway.generated.models.StateEntityDetailsResponseComponentDetails
import com.babylon.wallet.android.data.gateway.generated.models.StateEntityDetailsResponseItem
import com.babylon.wallet.android.domain.model.resources.metadata.MetadataItem.Companion.consume
import com.babylon.wallet.android.domain.model.resources.metadata.OwnerBadgeMetadataItem
import com.babylon.wallet.android.domain.model.resources.metadata.OwnerKeyHashesMetadataItem
import rdx.works.profile.data.model.pernetwork.Network

val StateEntityDetailsResponseItem.fungibleResourceAddresses: List<String>
    get() = fungibleResources?.items?.map { it.resourceAddress }.orEmpty()

val StateEntityDetailsResponseItem.nonFungibleResourceAddresses: List<String>
    get() = nonFungibleResources?.items?.map { it.resourceAddress }.orEmpty()

val StateEntityDetailsResponseItem.isEntityActive: Boolean
    get() {
        val metaDataItems = metadata.asMetadataItems().toMutableList()
        val ownerKeys = metaDataItems.consume<OwnerKeyHashesMetadataItem>()
        val ownerBadge = metaDataItems.consume<OwnerBadgeMetadataItem>()
        return listOfNotNull(ownerKeys?.updatedAtStateVersion, ownerBadge?.updatedAtStateVersion).any {
            it != 0L
        }
    }

val StateEntityDetailsResponseItem.defaultDepositRule: DefaultDepositRule?
    get() = when (details) {
        is StateEntityDetailsResponseComponentDetails -> {
            details.state?.defaultDepositRule
        }

        else -> null
    }

fun DefaultDepositRule.toProfileDepositRule(): Network.Account.OnLedgerSettings.ThirdPartyDeposits.DepositRule {
    return when (this) {
        DefaultDepositRule.Accept -> Network.Account.OnLedgerSettings.ThirdPartyDeposits.DepositRule.AcceptAll
        DefaultDepositRule.Reject -> Network.Account.OnLedgerSettings.ThirdPartyDeposits.DepositRule.DenyAll
        DefaultDepositRule.AllowExisting -> Network.Account.OnLedgerSettings.ThirdPartyDeposits.DepositRule.AcceptKnown
    }
}
