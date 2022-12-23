package com.babylon.wallet.android.domain.common

import kotlinx.coroutines.flow.Flow

interface OneOffEventHandler<T : OneOffEvent> {
    suspend fun sendEvent(event: T)
    val oneOffEvent: Flow<T>
}
