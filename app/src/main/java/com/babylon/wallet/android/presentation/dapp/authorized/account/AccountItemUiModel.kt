package com.babylon.wallet.android.presentation.dapp.authorized.account

import com.babylon.wallet.android.data.dapp.model.AccountsRequestResponseItem
import com.radixdlt.sargon.Account
import com.radixdlt.sargon.AccountAddress
import com.radixdlt.sargon.AppearanceId
import com.radixdlt.sargon.extensions.string

data class AccountItemUiModel(
    val address: AccountAddress,
    val displayName: String?,
    val appearanceID: AppearanceId,
    val isSelected: Boolean = false
)

fun Account.toUiModel(isSelected: Boolean = false) = AccountItemUiModel(
    address = address,
    displayName = displayName.value,
    appearanceID = appearanceId,
    isSelected = isSelected
)

fun List<AccountItemUiModel>.toDataModel(): AccountsRequestResponseItem? {
    if (this.isEmpty()) {
        return null
    }

    val accounts = map { accountItemUiModel ->
        AccountsRequestResponseItem.Account(
            address = accountItemUiModel.address.string,
            label = accountItemUiModel.displayName.orEmpty(),
            appearanceId = accountItemUiModel.appearanceID.value.toInt()
        )
    }

    return AccountsRequestResponseItem(
        accounts = accounts,
        challenge = null,
        proofs = null
    )
}
