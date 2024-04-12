package com.babylon.wallet.android.domain.model.assets

import rdx.works.core.domain.assets.Assets
import rdx.works.core.domain.resources.AccountDetails
import rdx.works.core.domain.resources.metadata.AccountType
import rdx.works.profile.data.model.pernetwork.Network

data class AccountWithAssets(
    val account: Network.Account,
    val details: AccountDetails? = null,
    val assets: Assets? = null,
) {

    val isDappDefinitionAccountType: Boolean
        get() = details?.accountType == AccountType.DAPP_DEFINITION
}
