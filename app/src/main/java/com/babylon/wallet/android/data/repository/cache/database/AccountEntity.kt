package com.babylon.wallet.android.data.repository.cache.database

import androidx.room.ColumnInfo
import androidx.room.Entity
import com.babylon.wallet.android.domain.model.resources.metadata.AccountTypeMetadataItem
import java.time.Instant

@Entity(primaryKeys = ["address"])
data class AccountEntity(
    val address: String,
    @ColumnInfo("account_type")
    val accountType: AccountTypeMetadataItem.AccountType?,
    val synced: Instant?,
    @ColumnInfo("state_version")
    val stateVersion: Long?
)
