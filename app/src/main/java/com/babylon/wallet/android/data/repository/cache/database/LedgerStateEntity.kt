package com.babylon.wallet.android.data.repository.cache.database

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
data class LedgerStateEntity(
    @PrimaryKey
    val id: Int = 1,
    @ColumnInfo("state_version")
    val stateVersion: Long
) {
    init {
        require(id == 1)
    }
}
