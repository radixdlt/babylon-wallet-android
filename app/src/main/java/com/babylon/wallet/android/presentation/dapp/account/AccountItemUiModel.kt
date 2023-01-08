package com.babylon.wallet.android.presentation.dapp.account

import com.babylon.wallet.android.domain.model.AccountResources

data class AccountItemUiModel(
    val address: String,
    val displayName: String,
    val appearanceID: Int,
    val isSelected: Boolean = false
)

fun AccountResources.toUiModel(isSelected: Boolean) = AccountItemUiModel(
    address = address,
    displayName = displayName,
    appearanceID = appearanceID,
    isSelected = isSelected
)
