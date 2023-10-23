package com.babylon.wallet.android.data.repository.cache.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
class StateVersionEntity(
    val version: Long,

    // Ensure single row for this entity
    @PrimaryKey
    val id: Int = 1,
) {

    init {
        require(id == 1)
    }

}
