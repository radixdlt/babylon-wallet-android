package com.babylon.wallet.android.data.repository.cache.database

import java.time.Instant

data class SyncInfo(
    val synced: Instant,
    val stateVersion: Long
)
