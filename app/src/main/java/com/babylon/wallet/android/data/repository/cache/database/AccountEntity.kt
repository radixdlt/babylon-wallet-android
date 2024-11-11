package com.babylon.wallet.android.data.repository.cache.database

import androidx.room.ColumnInfo
import androidx.room.Entity
import com.radixdlt.sargon.AccountAddress
import rdx.works.core.domain.resources.metadata.AccountType
import java.time.Instant

@Entity(primaryKeys = ["address"])
data class AccountEntity(
    val address: AccountAddress,
    val metadata: MetadataColumn?,
    val synced: Instant?,
    @ColumnInfo("state_version")
    val stateVersion: Long?,
    @ColumnInfo("first_transaction_date")
    val firstTransactionDate: Instant? = null
)
