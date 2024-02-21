package com.babylon.wallet.android.domain.model.resources

import com.babylon.wallet.android.domain.model.resources.metadata.AccountType
import java.time.Instant

data class AccountDetails(
    val stateVersion: Long,
    val accountType: AccountType? = null,
    val firstTransactionDate: Instant? = null
)
