package com.babylon.wallet.android

import android.app.Application
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import androidx.lifecycle.lifecycleScope
import com.babylon.wallet.android.utils.AppEvent
import com.babylon.wallet.android.utils.AppEventBus
import com.babylon.wallet.android.utils.DeviceSecurityHelper
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.launch
import rdx.works.profile.domain.CheckMnemonicIntegrityUseCase
import timber.log.Timber
import javax.inject.Inject

@HiltAndroidApp
class BabylonApplication : Application(), LifecycleEventObserver {

    @Inject
    lateinit var deviceSecurityHelper: DeviceSecurityHelper

    @Inject
    lateinit var checkMnemonicIntegrityUseCase: CheckMnemonicIntegrityUseCase

    @Inject
    lateinit var appEventBus: AppEventBus

    override fun onCreate() {
        super.onCreate()
        if (BuildConfig.DEBUG_MODE) {
            Timber.plant(Timber.DebugTree())
        }
        ProcessLifecycleOwner.get().lifecycle.addObserver(this)
    }

    override fun onTerminate() {
        ProcessLifecycleOwner.get().lifecycle.removeObserver(this)
        super.onTerminate()
    }

    override fun onStateChanged(source: LifecycleOwner, event: Lifecycle.Event) {
        when (event) {
            Lifecycle.Event.ON_RESUME -> {
                ProcessLifecycleOwner.get().lifecycleScope.launch {
                    // TODO this check feel a bit sketchy, still investigating if it can be done better
                    checkMnemonicIntegrityUseCase()
                    if (!deviceSecurityHelper.isDeviceSecure()) {
                        appEventBus.sendEvent(AppEvent.AppNotSecure, delayMs = 500)
                    }
                }
            }

            else -> {}
        }
    }
}
