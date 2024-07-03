package com.babylon.wallet.android.presentation.transaction.model

import com.radixdlt.sargon.Account
import com.radixdlt.sargon.DepositRule
import rdx.works.core.domain.resources.Resource

data class AccountWithDepositSettingsChanges(
    val account: Account,
    val defaultDepositRule: DepositRule? = null,
    val assetChanges: List<AssetPreferenceChange> = emptyList(),
    val depositorChanges: List<DepositorPreferenceChange> = emptyList()
) {
    data class AssetPreferenceChange(
        val change: ChangeType,
        val resource: Resource? = null
    ) {
        enum class ChangeType {
            Allow, Disallow, Clear
        }
    }

    data class DepositorPreferenceChange(
        val change: ChangeType,
        val resource: Resource? = null
    ) {
        enum class ChangeType {
            Add, Remove
        }
    }

    val onlyDepositRuleChanged: Boolean
        get() = assetChanges.isEmpty() && depositorChanges.isEmpty()
}
