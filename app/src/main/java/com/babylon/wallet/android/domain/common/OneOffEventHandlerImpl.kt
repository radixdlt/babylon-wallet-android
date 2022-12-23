package com.babylon.wallet.android.domain.common

import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlin.reflect.KProperty

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
