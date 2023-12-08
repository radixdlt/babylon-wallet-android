package com.babylon.wallet.android.domain.model.resources

import com.babylon.wallet.android.domain.model.resources.metadata.AccountType

data class AccountDetails(
    val stateVersion: Long,
    val accountType: AccountType? = null
)
