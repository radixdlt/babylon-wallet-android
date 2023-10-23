package com.babylon.wallet.android.data.repository.cache.database

import java.time.Instant

interface CachedEntity {
    val synced: Instant
    val stateVersion: Long
}
