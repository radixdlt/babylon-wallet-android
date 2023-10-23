package com.babylon.wallet.android.data.repository.cache.database

import androidx.room.ColumnInfo
import androidx.room.Entity
import com.babylon.wallet.android.domain.model.resources.AccountDetails
import com.babylon.wallet.android.domain.model.resources.metadata.AccountTypeMetadataItem
import java.time.Instant

@Entity(primaryKeys = ["address"])
data class AccountEntity(
    val address: String,
    @ColumnInfo("account_type")
    val accountType: AccountTypeMetadataItem.AccountType?,
    val progress: AccountInfoProgress,
    override val synced: Instant,
    @ColumnInfo("state_version")
    override val stateVersion: Long
) : CachedEntity

enum class AccountInfoProgress {
    PENDING,
    UPDATED
}
