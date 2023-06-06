package com.babylon.wallet.android.presentation.dapp.authorized.account

import com.babylon.wallet.android.data.dapp.model.AccountsRequestResponseItem
import rdx.works.profile.data.model.pernetwork.Network

data class AccountItemUiModel(
    val address: String,
    val displayName: String?,
    val appearanceID: Int,
    val isSelected: Boolean = false
)

fun Network.Account.toUiModel(isSelected: Boolean = false) = AccountItemUiModel(
    address = address,
    displayName = displayName,
    appearanceID = appearanceID,
    isSelected = isSelected
)

fun List<AccountItemUiModel>.toDataModel(): AccountsRequestResponseItem? {
    if (this.isEmpty()) {
        return null
    }

    val accounts = map { accountItemUiModel ->
        AccountsRequestResponseItem.Account(
            address = accountItemUiModel.address,
            label = accountItemUiModel.displayName.orEmpty(),
            appearanceId = accountItemUiModel.appearanceID
        )
    }

    return AccountsRequestResponseItem(
        accounts = accounts,
        challenge = null,
        proofs = null
    )
}
