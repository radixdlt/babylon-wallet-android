package com.babylon.wallet.android.presentation.dapp.authorized.account

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
