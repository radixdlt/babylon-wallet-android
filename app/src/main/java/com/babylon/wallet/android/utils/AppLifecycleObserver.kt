package com.babylon.wallet.android.utils

import android.os.Build
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import com.babylon.wallet.android.di.coroutines.ApplicationScope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

interface AppLifecycleObserver : DefaultLifecycleObserver {

    val lifecycleEvents: MutableSharedFlow<Lifecycle.Event>

    /**
     * Whether one of the app's screens is in front of the person (the top resumed activity), or null until known. On
     * the recent apps screen the app stays resumed, shown live on its card, and is paused and stopped only some 50 ms
     * before it is killed when swiped away: it leaves the front, though, as soon as that screen opens.
     */
    val inFront: StateFlow<Boolean?>

    /** An activity of the app became the top resumed one, or stopped being it (`Activity.onTopResumedActivityChanged`). */
    fun onTopResumedActivityChanged(activity: Any, isTop: Boolean)
}

class AppLifecycleObserverImpl @Inject constructor(
    @ApplicationScope private val scope: CoroutineScope
) : AppLifecycleObserver {

    override val lifecycleEvents = MutableSharedFlow<Lifecycle.Event>()

    private val topActivities = mutableSetOf<Int>()
    private val _inFront = MutableStateFlow<Boolean?>(null)
    override val inFront: StateFlow<Boolean?> = _inFront.asStateFlow()

    // Called on the main thread, as the activities' callbacks are.
    override fun onTopResumedActivityChanged(activity: Any, isTop: Boolean) {
        val id = System.identityHashCode(activity)
        if (isTop) topActivities += id else topActivities -= id
        _inFront.value = topActivities.isNotEmpty()
    }

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
        // Before Android 10 there is no top resumed activity: a resumed app is in front.
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) _inFront.value = true
        onLifecycleEvent(Lifecycle.Event.ON_RESUME)
    }

    override fun onPause(owner: LifecycleOwner) {
        super.onPause(owner)
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) _inFront.value = false
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
