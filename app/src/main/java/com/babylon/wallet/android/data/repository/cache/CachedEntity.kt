package com.babylon.wallet.android.data.repository.cache

import androidx.room.ColumnInfo
import java.time.Instant

interface CachedEntity {
    val updatedAt: Instant
    val epoch: Long
}


