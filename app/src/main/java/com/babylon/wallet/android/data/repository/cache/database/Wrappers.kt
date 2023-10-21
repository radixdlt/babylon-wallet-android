package com.babylon.wallet.android.data.repository.cache.database

import androidx.room.ColumnInfo
import androidx.room.Embedded
import java.math.BigDecimal

data class AccountResourceWrapper(
    @ColumnInfo("account_address")
    val address: String,
    val amount: BigDecimal,
    @Embedded
    val resource: ResourceEntity
)
