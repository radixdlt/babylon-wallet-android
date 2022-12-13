package com.babylon.wallet.android.utils

import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlin.reflect.KProperty

class SingleEventHandler<T> {

    private val eventChannel = Channel<T>(Channel.BUFFERED)
    private val eventFlow = eventChannel.receiveAsFlow()

    operator fun getValue(thisRef: Any?, property: KProperty<*>): Flow<T> {
        return eventFlow
    }

    suspend fun sendEvent(event: T) {
        eventChannel.send(event)
    }
}
