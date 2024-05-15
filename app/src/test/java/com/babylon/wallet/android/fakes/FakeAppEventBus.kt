package com.babylon.wallet.android.fakes

import com.babylon.wallet.android.utils.AppEvent
import com.babylon.wallet.android.utils.AppEventBus
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

class FakeAppEventBus : AppEventBus {

    private val _flow = MutableSharedFlow<AppEvent>()
    override val events: Flow<AppEvent>
        get() = _flow.asSharedFlow()

    override suspend fun sendEvent(event: AppEvent, delayMs: Long) {}
}