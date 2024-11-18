package com.babylon.wallet.android.domain.model.assets

import com.radixdlt.sargon.Account
import rdx.works.core.domain.assets.Assets
import rdx.works.core.domain.resources.AccountDetails
import rdx.works.core.domain.resources.metadata.AccountType

data class AccountWithAssets(
    val account: Account,
    val details: AccountDetails? = null,
    val assets: Assets? = null,
) {

    val isDappDefinitionAccountType: Boolean
        get() = details?.accountType == AccountType.DAPP_DEFINITION
}
