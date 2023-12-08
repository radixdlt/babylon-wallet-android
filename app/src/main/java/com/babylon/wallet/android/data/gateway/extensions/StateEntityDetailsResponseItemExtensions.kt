package com.babylon.wallet.android.data.gateway.extensions

import com.babylon.wallet.android.data.gateway.generated.models.DefaultDepositRule
import com.babylon.wallet.android.data.gateway.generated.models.StateEntityDetailsResponseComponentDetails
import com.babylon.wallet.android.data.gateway.generated.models.StateEntityDetailsResponseItem
import com.babylon.wallet.android.domain.model.resources.metadata.ownerBadge
import com.babylon.wallet.android.domain.model.resources.metadata.ownerKeyHashes
import rdx.works.profile.data.model.pernetwork.Network

val StateEntityDetailsResponseItem.isEntityActive: Boolean
    get() {
        val metaDataItems = metadata.toMetadata()
        val ownerKeys = metaDataItems.ownerKeyHashes()
        val ownerBadge = metaDataItems.ownerBadge()
        return listOfNotNull(ownerKeys?.lastUpdatedAtStateVersion, ownerBadge?.lastUpdatedAtStateVersion).any {
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
