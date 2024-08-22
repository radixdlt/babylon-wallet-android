package com.babylon.wallet.android.domain.model

import com.radixdlt.sargon.AccountAddress
import com.radixdlt.sargon.ResourceAddress

data class AccountDepositResourceRules(
    val canDepositAll: Boolean,
    val accountAddress: AccountAddress,
    val resourceRules: Set<ResourceDepositRule>
) {
    data class ResourceDepositRule(
        val resourceAddress: ResourceAddress,
        val isDepositAllowed: Boolean
    )

    fun canDeposit(resourceAddress: ResourceAddress): Boolean {
        return resourceRules.find { it.resourceAddress == resourceAddress }?.isDepositAllowed ?: true
    }
}
