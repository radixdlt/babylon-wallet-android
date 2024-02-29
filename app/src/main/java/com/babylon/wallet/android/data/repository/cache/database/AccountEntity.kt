package com.babylon.wallet.android.data.repository.cache.database

import androidx.room.ColumnInfo
import androidx.room.Entity
import rdx.works.core.domain.resources.metadata.AccountType
import java.time.Instant

@Entity(primaryKeys = ["address"])
data class AccountEntity(
    val address: String,
    @ColumnInfo("account_type")
    val accountType: AccountType?,
    val synced: Instant?,
    @ColumnInfo("state_version")
    val stateVersion: Long?,
    @ColumnInfo("first_transaction_date")
    val firstTransactionDate: Instant? = null
)
