package com.babylon.wallet.android.presentation.common

import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlin.reflect.KProperty

interface OneOffEvent

interface OneOffEventHandler<T : OneOffEvent> {
    suspend fun sendEvent(event: T)
    val oneOffEvent: Flow<T>
}

class OneOffEventHandlerImpl<T : OneOffEvent> : OneOffEventHandler<T> {

    private val eventChannel = Channel<T>(Channel.BUFFERED)
    override val oneOffEvent = eventChannel.receiveAsFlow()

    operator fun getValue(thisRef: Any?, property: KProperty<*>): Flow<T> {
        return oneOffEvent
    }

    override suspend fun sendEvent(event: T) {
        eventChannel.send(event)
    }
}
