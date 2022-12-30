package com.babylon.wallet.android.utils

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import javax.inject.Inject

class AppEventBus @Inject constructor() {

    private val _events = MutableSharedFlow<AppEvent>()
    val events = _events.asSharedFlow()

    suspend fun sendEvent(event: AppEvent) {
        _events.emit(event)
    }
}

sealed interface AppEvent {
    object GotFreeXrd : AppEvent
}
