package com.babylon.wallet.android.data.repository.cache

import java.time.Instant

data class SyncInfo(
    val synced: Instant,
    val epoch: Long
)
