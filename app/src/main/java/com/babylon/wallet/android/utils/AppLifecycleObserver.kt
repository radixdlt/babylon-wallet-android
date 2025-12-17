package com.babylon.wallet.android.utils

import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import com.babylon.wallet.android.di.coroutines.ApplicationScope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

interface AppLifecycleObserver : DefaultLifecycleObserver {

    val lifecycleEvents: MutableSharedFlow<Lifecycle.Event>
}

class AppLifecycleObserverImpl @Inject constructor(
    @ApplicationScope private val scope: CoroutineScope
) : AppLifecycleObserver {

    override val lifecycleEvents = MutableSharedFlow<Lifecycle.Event>()

    override fun onCreate(owner: LifecycleOwner) {
        super.onCreate(owner)
        onLifecycleEvent(Lifecycle.Event.ON_CREATE)
    }

    override fun onStart(owner: LifecycleOwner) {
        super.onStart(owner)
        onLifecycleEvent(Lifecycle.Event.ON_START)
    }

    override fun onResume(owner: LifecycleOwner) {
        super.onResume(owner)
        onLifecycleEvent(Lifecycle.Event.ON_RESUME)
    }

    override fun onPause(owner: LifecycleOwner) {
        super.onPause(owner)
        onLifecycleEvent(Lifecycle.Event.ON_PAUSE)
    }

    override fun onStop(owner: LifecycleOwner) {
        super.onStop(owner)
        onLifecycleEvent(Lifecycle.Event.ON_STOP)
    }

    override fun onDestroy(owner: LifecycleOwner) {
        super.onDestroy(owner)
        onLifecycleEvent(Lifecycle.Event.ON_DESTROY)
    }

    private fun onLifecycleEvent(event: Lifecycle.Event) {
        scope.launch { lifecycleEvents.emit(event) }
    }
}
