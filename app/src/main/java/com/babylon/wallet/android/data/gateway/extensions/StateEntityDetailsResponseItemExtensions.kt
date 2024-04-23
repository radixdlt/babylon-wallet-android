package com.babylon.wallet.android.data.gateway.extensions

import com.babylon.wallet.android.data.gateway.generated.models.DefaultDepositRule
import com.babylon.wallet.android.data.gateway.generated.models.StateEntityDetailsResponseComponentDetails
import com.babylon.wallet.android.data.gateway.generated.models.StateEntityDetailsResponseItem
import com.radixdlt.sargon.DepositRule
import rdx.works.core.domain.resources.metadata.ownerBadge
import rdx.works.core.domain.resources.metadata.ownerKeyHashes

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

fun DefaultDepositRule.toProfileDepositRule(): DepositRule {
    return when (this) {
        DefaultDepositRule.Accept -> DepositRule.ACCEPT_ALL
        DefaultDepositRule.Reject -> DepositRule.DENY_ALL
        DefaultDepositRule.AllowExisting -> DepositRule.ACCEPT_KNOWN
    }
}
