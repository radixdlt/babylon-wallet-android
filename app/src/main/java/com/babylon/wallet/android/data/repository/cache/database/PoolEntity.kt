package com.babylon.wallet.android.data.repository.cache.database

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
data class PoolEntity(
    @PrimaryKey
    val address: String,
    @ColumnInfo("state_version")
    val stateVersion: Long
)
