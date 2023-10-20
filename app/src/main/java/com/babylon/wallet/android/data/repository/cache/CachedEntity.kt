package com.babylon.wallet.android.data.repository.cache

import java.time.Instant

interface CachedEntity {
    val synced: Instant
    val epoch: Long
}
