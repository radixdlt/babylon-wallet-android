package com.babylon.wallet.android.presentation.dapp.authorized.account

import com.radixdlt.sargon.Account
import com.radixdlt.sargon.AccountAddress
import com.radixdlt.sargon.AppearanceId

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