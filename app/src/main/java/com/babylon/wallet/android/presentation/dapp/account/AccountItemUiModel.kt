package com.babylon.wallet.android.presentation.dapp.account

import rdx.works.profile.data.model.pernetwork.OnNetwork

data class AccountItemUiModel(
    val address: String,
    val displayName: String?,
    val appearanceID: Int,
    val isSelected: Boolean = false
)

fun OnNetwork.Account.toUiModel(isSelected: Boolean) = AccountItemUiModel(
    address = address,
    displayName = displayName,
    appearanceID = appearanceID,
    isSelected = isSelected
)
