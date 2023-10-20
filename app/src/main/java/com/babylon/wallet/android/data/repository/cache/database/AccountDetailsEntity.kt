package com.babylon.wallet.android.data.repository.cache.database

import androidx.room.ColumnInfo
import androidx.room.Entity
import com.babylon.wallet.android.data.repository.cache.CachedEntity
import com.babylon.wallet.android.domain.model.resources.AccountDetails
import com.babylon.wallet.android.domain.model.resources.metadata.AccountTypeMetadataItem
import java.time.Instant

@Entity(primaryKeys = ["address"])
data class AccountDetailsEntity(
    val address: String,
    @ColumnInfo("account_type")
    val accountType: AccountTypeMetadataItem.AccountType?,
    override val synced: Instant,
    override val epoch: Long
) : CachedEntity {

    fun toAccountDetails() = AccountDetails(
        typeMetadataItem = accountType?.let { AccountTypeMetadataItem(it) }
    )

}
